package com.spotify.music.andlib;

import android.os.Parcel;
import android.os.Parcelable;

public class MyParcelable implements Parcelable {
    public static final String TEST = com.spotify.music.innerandlib.InnerAndLib.TEST;
    public MyParcelable() {
    }

    protected MyParcelable(Parcel in) {
    }

    public static final Creator<MyParcelable> CREATOR = new Creator<MyParcelable>() {
        @Override
        public MyParcelable createFromParcel(Parcel in) {
            return new MyParcelable(in);
        }

        @Override
        public MyParcelable[] newArray(int size) {
            return new MyParcelable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}
