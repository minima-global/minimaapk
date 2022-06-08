package com.minima.android.ui.maxima.contacts;

import org.minima.utils.json.JSONObject;

import java.io.Serializable;

public class Contact implements Serializable {

    public long mID          = 0;

    public String mName      = "";
    public String mPublicKey = "";
    public String mAddress   = "";
    public String mMyAddress   = "";

    public String mMyChainTip;
    public String mTheirChainTip;

    public String mMinimaAddress   = "";

    public  long  mLastSeen = System.currentTimeMillis();

    public boolean mSameChain = false;

    public Contact(){}

    public Contact(String zName){
        mName = zName;
    }

    public Contact(JSONObject zJSONContact){
        mID         = (long)zJSONContact.get("id");
        mPublicKey  = (String)zJSONContact.get("publickey");
        mAddress    = (String)zJSONContact.get("currentaddress");
        mMyAddress  = (String)zJSONContact.get("myaddress");
        mLastSeen   = (long)zJSONContact.get("lastseen");

        //mMyChainTip = (String)zJSONContact.get("chaintip");
        mSameChain  = (boolean)zJSONContact.get("samechain");

        //Get the Extra Data
        JSONObject extradata    = (JSONObject)zJSONContact.get("extradata");
        mName                   = (String)extradata.get("name");
        mMinimaAddress          = (String)extradata.get("minimaaddress");
        //mTheirChainTip          = (String)extradata.get("topblock");
    }

    public long getLastSeen(){
        return mLastSeen;
    }

    public boolean getChainStatus(){
        return mSameChain;
    }
}
