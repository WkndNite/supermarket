package com.example.fpgrowth;

import java.io.FileWriter;
import java.io.IOException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.ml.fpm.FPGrowth;
import org.apache.spark.ml.fpm.FPGrowthModel;

public class FPGrowthApp {
        public static void main(String[] args) throws IOException {
                if (args.length < 2) {
                        System.err.println("Usage: FPGrowthApp <minSupport> <inputPath>");
                        System.exit(1);
                }

                double minSupport = Double.parseDouble(args[0]);
                String inputPath = args[1];
                String outputDir = args.length >= 3 ? args[2] : "/opt/output";

                SparkSession spark = SparkSession.builder()
                                .appName("Supermarket FP-Growth Analysis")
                                .getOrCreate();

                Dataset<Row> data = spark.read().text(inputPath);
                Dataset<Row> df = data.selectExpr("split(value, ' ') as items");

                FPGrowth fpGrowth = new FPGrowth()
                                .setItemsCol("items")
                                .setMinSupport(minSupport)
                                .setMinConfidence(0.6);

                FPGrowthModel model = fpGrowth.fit(df);

                // 显示频繁项集，false参数防止截断，完整显示
                System.out.println("Frequent Itemsets:");
                model.freqItemsets().show(false);

                // 显示根据频繁项集生成的关联规则
                System.out.println("Association Rules:");
                model.associationRules().show(false);

                // 预测每个交易可能出现的后续商品，展示transform结果
                System.out.println("Transform:");
                model.transform(df).show(false);

                // 保存频繁项集到文件
                String freqItemsetsStr = model.freqItemsets().toJSON().collectAsList().toString();
                try (FileWriter fw = new FileWriter(outputDir + "/freqItemsets.json")) {
                        fw.write(freqItemsetsStr);
                }

                // 保存关联规则到文件
                String assocRulesStr = model.associationRules().toJSON().collectAsList().toString();
                try (FileWriter fw = new FileWriter(outputDir + "/associationRules.json")) {
                        fw.write(assocRulesStr);
                }

                // 保存预测结果到文件
                String transformStr = model.transform(df).toJSON().collectAsList().toString();
                try (FileWriter fw = new FileWriter(outputDir + "/transform.json")) {
                        fw.write(transformStr);
                }
                System.out.println("Results saved to " + outputDir);
                // 关闭Spark会话
                System.out.println("Analysis completed successfully.");
                System.out.println("Spark session stopped.");

                spark.stop();
        }
}
