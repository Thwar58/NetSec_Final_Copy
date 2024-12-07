/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ClientFinal;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author burli
 */
public class mainController implements ActionListener {

    //this is the gui we will be controlling
    private static GuiMainEdit mainEdit;
    //set this is true when JButton# is selected
    private static boolean sendMessage;
    //the message being sent in pure string form
    private static String aMessage;
    //if there is a message to be sent
    private static boolean hasMessage;
    //true if there is a new file to be sent
    private static boolean hasFile;
    //the path of the file
    private static String path;
    //the file in byte array representation
    private static File file;
    //the name of the file
    private static String fName;
    //if the download is pressed
    private static boolean wantsToDownload;
    //the postion in the arraylists where the file lives
    private static int positionOfFile;

    public mainController() {
        mainEdit = new GuiMainEdit();
        sendMessage = false;
        this.mainEdit.getMessageButton().addActionListener(this);
        this.mainEdit.getSendFileButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hasFile = true;
            }
        });
        this.mainEdit.getSelectFileButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                file = new File("");
                JFileChooser pickFile = new JFileChooser();
                pickFile.setCurrentDirectory(new File(System.getProperty("user.dir")));
                pickFile.setDialogTitle("Select a  File");
                int result = pickFile.showSaveDialog(null);
                //if the user click on save in Jfilechooser
                if (result == JFileChooser.APPROVE_OPTION) {
                    file = pickFile.getSelectedFile();
                    path = file.getAbsolutePath();
                    fName = file.getName();
                    mainEdit.setFileText(fName);
                }
            }
        });

        this.mainEdit.getDownloadButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                wantsToDownload = true;
                positionOfFile = mainEdit.getIndex();
            }

        });
        hasMessage = false;
        wantsToDownload = false;
        hasFile = false;
    }

    public GuiMainEdit getGui() {
        return mainEdit;
    }

    public void runGUI() {
        this.mainEdit.open();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.aMessage = this.mainEdit.getMessageText();
        hasMessage = true;
    }

    public String getMessage() {
        return this.aMessage;
    }

    public void setHasMessage(boolean boolin) {
        this.hasMessage = boolin;
    }

    public static boolean getHasMessage() {
        return hasMessage;
    }

    public void updateFileBox(ArrayList<String> fileNames) {
        this.mainEdit.updateFileBox(fileNames);
    }

    public void updateConnectedBox(String ourUser, ArrayList<String> usersAndKeys) {
        ArrayList<String> users = new ArrayList<>();
        users.add(ourUser);
        for (int i = 0; i < usersAndKeys.size(); i++) {
            if (i % 2 == 0) {
                String aUser = usersAndKeys.get(i);
                users.add(aUser);
            }
        }
        this.mainEdit.updateConnectedBox(users);
    }

    public void updateMessageBox(String message) {
        this.mainEdit.updateMessageBox(message);
    }

    public boolean getHasFile() {
        return hasFile;
    }

    public void setHasFile(boolean input) {
        hasFile = input;
    }

    public byte[] getFile() {
        byte[] bytes = new byte[(int) file.length()];
        FileInputStream fis = null;
        try {

            fis = new FileInputStream(file);

            //read file into bytes[]
            fis.read(bytes);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return bytes;
    }

    public String getPath() {
        return path;
    }

    public String getFileName() {
        return fName;
    }

    public int getSelectedPos(){
        return positionOfFile;
    }

    public boolean getWantsToDownload(){
        return wantsToDownload;
    }

    public void setWantsToDownload(boolean bool){
        wantsToDownload = bool;
    }
}
