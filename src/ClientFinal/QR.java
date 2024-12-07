/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientFinal;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import javax.imageio.ImageIO;

/**
 *
 * @author burli
 */
public class QR {

    public static BufferedImage code;
    
    public QR(String userName, String base32Secret) throws UnsupportedEncodingException, WriterException{
        code = makeQR(userName, base32Secret);
    }

    //https://www.youtube.com/watch?v=238LfSBwvbs partially taken from here, Matrix import was not working
    public static BufferedImage makeQR(String userName, String base32Secret) throws UnsupportedEncodingException, WriterException {      
        String infoQR = "otpauth://totp/" + userName + "?secret=" + base32Secret + "&algorithm=SHA1" + "&digits=6" + "&period=30";
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(infoQR, BarcodeFormat.QR_CODE, 300, 300);
        BufferedImage aQR = new BufferedImage(bitMatrix.getWidth(), bitMatrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < bitMatrix.getWidth(); x++) {
            for (int y = 0; y < bitMatrix.getHeight(); y++) {
                int color;
                if (bitMatrix.get(x, y)) {
                    color = Color.BLACK.hashCode();
                } else {
                    color = Color.WHITE.hashCode();
                }
                aQR.setRGB(x, y, color);
            }
        }
        return aQR;
    }
    
    public BufferedImage getQR(){
        return code;
    }
}
