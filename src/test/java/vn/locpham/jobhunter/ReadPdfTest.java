package vn.locpham.jobhunter;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import java.io.File;

public class ReadPdfTest {
    @Test
    public void testReadPdf() throws Exception {
        File file = new File("c:\\javaRestFullApi\\Upload\\resume\\1778294548843-cvmau.pdf");
        PDDocument document = Loader.loadPDF(file);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        System.out.println("--- PDF TEXT START ---");
        System.out.println(text);
        System.out.println("--- PDF TEXT END ---");
        document.close();
    }
}
