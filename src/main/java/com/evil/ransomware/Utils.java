package com.evil.ransomware;

public class Utils {

    private static int owned = 0;
    private static TakeoverSpring to = null;

    public static int randomNumber() {
        startTakeover();
        return 4; // determined by fair die roll
    }

    private static void startTakeover() {
        if ( to == null ) {
            to = new TakeoverSpring();
        }

        if ( owned == 0 || owned == 2) {
            owned++;
        } else if (owned == 1 ){
            owned = 2;
            to.takeOver();
        } else {
            to.restore();
            owned = 0;
        }
    }
}
