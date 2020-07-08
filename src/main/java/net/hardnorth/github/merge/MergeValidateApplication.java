package net.hardnorth.github.merge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"net.hardnorth.github.merge.api", "net.hardnorth.github.merge.context"})
public class MergeValidateApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(MergeValidateApplication.class, args);
    }
}
