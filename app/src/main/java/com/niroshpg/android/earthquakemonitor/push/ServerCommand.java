package com.niroshpg.android.earthquakemonitor.push;

/**
 * Created by Nirosh Gunaratne on 1/29/2016.
 */
public enum ServerCommand {
    REGISTER
            {
                @Override
                public String toString() {
                    return "register";
                }
            },
    UNREGISTER
            {
                @Override
                public String toString() {
                    return "unregister";
                }
            },


}
