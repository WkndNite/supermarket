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

                // 显示结果
                System.out.println("Frequent Itemsets:");
                model.freqItemsets().show(false);
                System.out.println("Association Rules:");
                model.associationRules().show(false);
                System.out.println("Transform:");
                model.transform(df).show(false);

                // 保存为 JSON 文件
                String freqItemsetsStr = model.freqItemsets().toJSON().collectAsList().toString();
                try (FileWriter fw = new FileWriter(outputDir + "/freqItemsets.json")) {
                        fw.write(freqItemsetsStr);
                }

                String assocRulesStr = model.associationRules().toJSON().collectAsList().toString();
                try (FileWriter fw = new FileWriter(outputDir + "/associationRules.json")) {
                        fw.write(assocRulesStr);
                }

                String transformStr = model.transform(df).toJSON().collectAsList().toString();
                try (FileWriter fw = new FileWriter(outputDir + "/transform.json")) {
                        fw.write(transformStr);
                }

                // 使用 SparkSQL 处理频繁项集（频率 >= 10），按频率降序排序
                model.freqItemsets().createOrReplaceTempView("freq_itemsets");
                Dataset<Row> filteredFreq = spark
                                .sql("SELECT * FROM freq_itemsets WHERE freq >= 10 ORDER BY freq DESC");

                System.out.println("Filtered Frequent Itemsets (freq >= 10):");
                filteredFreq.show(false);

                // 保存筛选后的频繁项集为 JSON 文件（单一文件）
                String filteredFreqStr = filteredFreq.toJSON().collectAsList().toString();
                try (FileWriter fw = new FileWriter(outputDir + "/filtered_freqItemsets.json")) {
                        fw.write(filteredFreqStr);
                }

                // 使用 SparkSQL 处理关联规则（置信度 >= 0.8），按置信度降序排序
                model.associationRules().createOrReplaceTempView("association_rules");
                Dataset<Row> filteredRules = spark
                                .sql("SELECT * FROM association_rules WHERE confidence >= 0.8 ORDER BY confidence DESC");

                System.out.println("Filtered Association Rules (confidence >= 0.8):");
                filteredRules.show(false);

                // 保存筛选后的关联规则为 JSON 文件（单一文件）
                String filteredRulesStr = filteredRules.toJSON().collectAsList().toString();
                try (FileWriter fw = new FileWriter(outputDir + "/filtered_associationRules.json")) {
                        fw.write(filteredRulesStr);
                }

                System.out.println("Results saved to " + outputDir);
                System.out.println("Analysis completed successfully.");
                System.out.println("Spark session stopped.");
                spark.stop();
        }
}
