package com.example.fpgrowth;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class SubmitSparkJob {

    public static void main(String[] args) {
        try {
            // ✅ 你可以在这里修改最小支持度
            String minSupport = "0.3";

            // ✅ 构造完整 docker + spark-submit 命令
            String[] command = {
                    "docker", "run", "--rm",
                    "--network", "supermarket_hadoopnet",
                    "-v",
                    System.getProperty("user.dir")
                            + "/supermarket-fpgrowth/target/supermarket-fpgrowth-1.0-SNAPSHOT-shaded.jar:/opt/app.jar",
                    "-v", System.getProperty("user.dir") + "/supermarket-fpgrowth/out:/opt/output",
                    "bitnami/spark:3.5.0",
                    "spark-submit",
                    "--master", "spark://spark:7077",
                    "--class", "com.example.fpgrowth.FPGrowthApp",
                    "/opt/app.jar",
                    minSupport,
                    "hdfs://namenode:9000/user/root/input/retail.dat",
                    "/opt/output"
            };

            System.out.println("▶ Running Spark job with support = " + minSupport);
            System.out.println("▶ Full command: \n" + String.join(" ", command));

            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true); // 合并标准输出和错误输出
            Process process = builder.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("✅ Spark job finished successfully.");
            } else {
                System.err.println("❌ Spark job failed with exit code " + exitCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
