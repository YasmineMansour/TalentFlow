package tests;

import utils.MyDataBase;

public class MainConnection {
    public static void main(String[] args) {
        MyDataBase.getInstance().getConnection();
        System.out.println("Connexion OK");
    }
}
