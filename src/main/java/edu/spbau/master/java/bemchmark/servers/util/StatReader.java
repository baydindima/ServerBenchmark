package edu.spbau.master.java.bemchmark.servers.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class StatReader {

    private static final class ArchInfo {
        private final ArrayList<Integer> clientTime = new ArrayList<>();
        private final ArrayList<Integer> requestTime = new ArrayList<>();
        private final ArrayList<Integer> workTime = new ArrayList<>();
    }


    public static void main(String[] args) throws FileNotFoundException {
        File dir = new File(".");
        File[] files = dir.listFiles();


        String suffix = "_delay_test";

        Map<String, ArchInfo> archsInfo = new HashMap<>();
        for (File file : files) {
            if (file.getName().endsWith(suffix)) {
                String archName = file.getName().replace(suffix, "");
                ArchInfo archInfo = new ArchInfo();
                Scanner scanner = new Scanner(new BufferedInputStream(new FileInputStream(file)));
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (!line.trim().isEmpty()) {
                        String[] values = line.split("\t");
                        archInfo.clientTime.add(Integer.parseInt(values[0]));
                        archInfo.requestTime.add(Integer.parseInt(values[1]));
                        archInfo.workTime.add(Integer.parseInt(values[2]));
                    }
                }
                archsInfo.put(archName, archInfo);
            }
        }
        int testCount = archsInfo.get("ASYNC_TCP").clientTime.size();


        for (String archName: archsInfo.keySet()) {
            System.out.print(archName + "\t");
        }
        System.out.println();


        for (int i = 0; i < testCount; i++) {
            for (ArchInfo archInfo : archsInfo.values()) {
                System.out.print(archInfo.workTime.get(i) + "\t");
            }
            System.out.println();
        }
    }
}
