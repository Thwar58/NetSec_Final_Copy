/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientFinal;

import com.google.zxing.WriterException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author burli
 */
public class LoginController implements ActionListener {

    public GuiLoginEdit guiLogin;
    public String createUser;
    public String createPass;
    public String loginUser;
    public String loginPass;
    public String OTP;
    public Boolean createAttempt = false;
    public Boolean loginAttempt = false;

    public LoginController() {
        GuiLoginEdit login = new GuiLoginEdit();
        this.guiLogin = login;
        this.guiLogin.getSignButton().addActionListener(this);
        this.guiLogin.getLoginButton().addActionListener(this);
        
    }

    public Boolean getCreateAttempt() {
        return createAttempt;
    }

    public void setCreateAttempt(Boolean create) {
        createAttempt = create;
    }

    public Boolean getLoginAttempt() {
        return loginAttempt;
    }

    public void setLoginAttempt(Boolean loginAttempt) {
        this.loginAttempt = loginAttempt;
    }

    public void runGUI() {
        this.guiLogin.open();
    }
    public void closeGUI(){this.guiLogin.dispose();}

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getCreatePass() {
        return createPass;
    }

    public void setCreatePass(String createPass) {
        this.createPass = createPass;
    }

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    public String getLoginPass() {
        return loginPass;
    }

    public void setLoginPass(String loginPass) {
        this.loginPass = loginPass;
    }

    public String getOTP() {
        return OTP;
    }

    public void setOTP(String OTP) {
        this.OTP = OTP;
    }
    
    public void generateQR(String username, String totp){
        try {
            QR genQR = new QR(username, totp);
            BufferedImage aQR = genQR.getQR();
            this.guiLogin.addQRToScreen(aQR);
            
            
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (WriterException ex) {
            Logger.getLogger(LoginController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == guiLogin.getSignButton()){
            this.createUser = guiLogin.getSignUser();
            this.createPass = guiLogin.getSignPass();
            this.createAttempt = true;
            
        }
        if (e.getSource() == guiLogin.getLoginButton()){
            this.loginUser = guiLogin.getLoginUser();
            this.loginPass = guiLogin.getLoginPass();
            this.OTP = guiLogin.getOTP();
            loginAttempt = true;
            
        }
        
    }

}
