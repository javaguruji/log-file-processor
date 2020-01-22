import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author badrikant.soni
 */
public class LogFileProcessor {

    static ThreadLocal<DateFormat> threadLocal = ThreadLocal.withInitial(() -> new SimpleDateFormat("HH:mm:ss"));

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<List<String>> futureList = new ArrayList<>();
        Future<Object> future = null;

        for (int i = 0; i < 19; i++) {
            final int fileNumber = i;
            future = executorService.submit(new Callable<Object>() {
                @Override
                public List<String> call() throws Exception {
                    String filePath = "/src/main/java/log" + fileNumber + ".text";
                    List<String> stringList = null;
                    try {
                        stringList = searchLogsInTimeRage(1523718000000L, 1523718907817L, filePath, ExceptionType.IllegalAgrumentsException);

                    } catch (IOException e) {
                        System.out.println("Exception occured while processng the log files");
                    }
                    return stringList;
                }
            });
            ArrayList<String> arrayList = (ArrayList<String>) future.get();
            futureList.add(arrayList);
        }
        executorService.shutdown();
        int count = 0;
        String startime = null;
        String endTime = null;
        String exception = null;

        for (List<String> stringFuture : futureList) {
            count = count + Integer.parseInt(stringFuture.get(3));
            exception = stringFuture.get(2);
            startime = stringFuture.get(0);
            endTime = stringFuture.get(1);
        }

        String content = startime + " " + endTime + " " + exception + " " + count;
        fileWriter(content);

    }

    private static List<String> searchLogsInTimeRage(Long startTime, Long endTime, String filePath, ExceptionType exceptionType) throws IOException {

        List<String> result = null;
        switch (exceptionType) {
            case NullPointerException:
                result = search(startTime, endTime, filePath, ExceptionType.NullPointerException);
                break;
            case IllegalAgrumentsException:
                result = search(startTime, endTime, filePath, ExceptionType.IllegalAgrumentsException);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return result;
    }

    private static List<String> search(Long startTime, Long endTime, String filePath, ExceptionType exceptionType) throws IOException {

        AtomicInteger atomicInteger = new AtomicInteger();
        Pattern pattern = Pattern.compile(exceptionType.name());
        Matcher matcher = pattern.matcher("");
        String property = System.getProperty("user.dir");
        BufferedReader br = null;
        String file = property + filePath;
        String line;
        try {
            br = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            System.err.println("Cannot read '" + file + "': " + e.getMessage());
        }
        ArrayList<String> arrayList = new ArrayList<String>();
        String[] split = null;
        String convertedStartTime = null;
        String convertedEndTime = null;
        String exception = null;
        while ((line = br.readLine()) != null) {
            split = line.split(" ");
            String time = split[1];
            Long longTime = Long.parseLong(time);
            convertedStartTime = convertEpochToDate(startTime);
            convertedEndTime = convertEpochToDate(endTime);
            if (longTime >= startTime && longTime <= endTime) {
                matcher.reset(line);
                if (matcher.find()) {
                    atomicInteger.incrementAndGet();
                    exception = split[2];
                }
            }
        }
        br.close();
        arrayList.add(convertedStartTime);
        arrayList.add(convertedEndTime);
        arrayList.add(exception);
        arrayList.add(String.valueOf(atomicInteger.get()));
        return arrayList;

    }

    private static String convertEpochToDate(Long longTime) {
        Date date = new Date(longTime);
        DateFormat format = threadLocal.get();
        format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        return format.format(date);
    }

    private static void fileWriter(String fileContent) throws IOException {
        String filePath = "/src/main/java/output" + ".text";
        String property = System.getProperty("user.dir");
        String file = property + filePath;
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(fileContent);
        fileWriter.close();
    }
}
