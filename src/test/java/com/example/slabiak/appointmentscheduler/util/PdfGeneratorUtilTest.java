// 测试说明：验证 PDF 生成工具可以输出包含发票内容的 PDF 数据。
package com.example.slabiak.appointmentscheduler.util;

import com.example.slabiak.appointmentscheduler.entity.Invoice;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfGeneratorUtilTest {

    @Test
    void shouldGeneratePdfFromInvoiceTemplate() {
        SpringTemplateEngine templateEngine = org.mockito.Mockito.mock(SpringTemplateEngine.class);
        Invoice invoice = new Invoice();
        when(templateEngine.process(eq("email/pdf/invoice"), any(Context.class)))
                .thenReturn("""
                        <!DOCTYPE html>
                        <html>
                        <head><meta charset="UTF-8" /></head>
                        <body><h1>Invoice</h1><p>Test invoice</p></body>
                        </html>
                        """);
        PdfGeneratorUtil util = new PdfGeneratorUtil(templateEngine, "http://localhost/");

        File pdf = util.generatePdfFromInvoice(invoice);

        // 检查点：验证该测试用例的预期结果。
        assertThat(pdf).isNotNull();
        assertThat(pdf).exists();
        assertThat(pdf.length()).isGreaterThan(0);
        pdf.delete();
        // 检查点：验证该测试用例的预期结果。
        verify(templateEngine).process(eq("email/pdf/invoice"), any(Context.class));
    }
}
