package com.atguigu.gmall.payment.test;

import java.util.Scanner;

public class sf {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int count = 0;
        if (n % 2 == 0){
            count = -(n/2);
        }else{
            count = n / 2 + 1;
        }
        System.out.println(count);
    }
}
