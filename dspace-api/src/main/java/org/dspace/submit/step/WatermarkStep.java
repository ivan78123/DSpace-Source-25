package org.dspace.submit.step;

import org.apache.log4j.Logger;
import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.core.Context;
import org.dspace.submit.AbstractProcessingStep;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.*;

public class WatermarkStep extends AbstractProcessingStep {

    private Logger logger;

    // error
    public static final int STATUS_NO_FILES_ERROR = 5;

    String uploadDir = configurationService.getProperty("upload.temp.dir");
    String watermark_image = configurationService.getProperty("watermark.image");
    String pdfPassword = configurationService.getProperty("pdf.watermark.password");

    @Override
    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo subInfo) throws ServletException, IOException, SQLException, AuthorizeException {

        String buttonPressed = Util.getSubmitButton(request, CANCEL_BUTTON);

        BitstreamFormatService bitstreamFormatService = null;

        String decision = request.getParameter("decision");

        if (decision != null && decision.equalsIgnoreCase("accept")
                && buttonPressed.equals(NEXT_BUTTON)){

            Item item = subInfo.getSubmissionItem().getItem();

            generateWatermarkPdf(watermark_image);

            File watermarkSourceFile = new File(uploadDir + "/tmp_watermark.pdf");

            PDDocument watermarkPdf = PDDocument.load( watermarkSourceFile );

            List<Bundle> bundles = itemService.getBundles(item, "ORIGINAL");

            for (Bundle bundle : bundles) {

                List<Bitstream> bitstreams = bundle.getBitstreams();

                for (Bitstream bitstream : bitstreams){

                    String name = bitstream.getName();
                    String description = bitstream.getDescription();
                    String fileSource = bitstream.getSource();

                    BitstreamFormat format = bitstream.getFormat(context);

                    if (format.getMIMEType().equalsIgnoreCase("application/pdf")) {

                        InputStream inputStream = bitstreamService.retrieve(context, bitstream);

                        PDDocument pdDocument = PDDocument.load(inputStream);

                        try {

                            HashMap<Integer, String> overlayProps = new HashMap<>();

                            for (int i = 0; i < pdDocument.getNumberOfPages(); i++) {

                                overlayProps.put(i + 1, uploadDir + "/tmp_watermark.pdf");
                            }

                            Overlay overlay = new Overlay();
                            overlay.setInputPDF(pdDocument);
                            overlay.setOverlayPosition(Overlay.Position.BACKGROUND);
                            overlay.overlay(overlayProps);

                            // Access Permission Modification
                            AccessPermission ap = new AccessPermission();
                            ap.setCanModify(false);

                            ap.setCanExtractContent(false);
                            ap.setCanExtractForAccessibility(false);
                            ap.setCanPrint(false);
                            ap.setCanPrintDegraded(false);
                            ap.setCanFillInForm(false);
                            ap.setCanModifyAnnotations(false);

                            StandardProtectionPolicy spp = new StandardProtectionPolicy(
                                    UUID.randomUUID().toString(), "", ap);

                            pdDocument.protect(spp);

                            pdDocument.save(uploadDir + "/Protected_" + name);

                            File newPdf = new File(uploadDir + "/Protected_" + name);

                            FileInputStream fileInputStream = new FileInputStream(newPdf);

                            Bitstream newBitstream = bitstreamService.create(context, fileInputStream);

                            newBitstream.setName(context, "Protected_" + name);

                            newBitstream.setDescription(context, description);
                            newBitstream.setSource(context, fileSource);
                            newBitstream.setFormat(context, format);

                            bitstreamService.update(context, newBitstream);

                            itemService.update(context, item);

                            bundleService.addBitstream(context, bundle, newBitstream);

                            bitstreamService.delete(context, bitstream);

                            newPdf.delete();

                        } catch (IOException e) {

                            e.printStackTrace();
                        }

                        inputStream.close();
                        pdDocument.close();
                    }
                } 
            }
            watermarkPdf.close();
            watermarkSourceFile.delete(); 
        }
        return 0;
    }

    @Override
    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo subInfo) throws ServletException {

        return 1;
    }

    private void generateWatermarkPdf(String watermarkImage) throws IOException {

        InputStream in = new FileInputStream(watermarkImage);
        BufferedImage bimg = ImageIO.read(in);

        float width = bimg.getWidth() ;
        float height = bimg.getHeight();

        PDPage page = new PDPage(new PDRectangle(width, height));

        PDDocument doc = new PDDocument();

        doc.addPage(page);

        PDImageXObject  pdImageXObject = JPEGFactory.createFromImage(doc, bimg, 0.5f, 10);

        PDPageContentStream contentStream = new PDPageContentStream(doc, page);

        contentStream.drawImage(pdImageXObject, 0, 0);

        contentStream.close();

        doc.save(uploadDir + "/tmp_watermark.pdf");
        doc.close();
    }
}
