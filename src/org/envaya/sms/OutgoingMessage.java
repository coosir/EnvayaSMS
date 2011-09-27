
package org.envaya.sms;

import org.envaya.sms.receiver.OutgoingMessageRetry;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;
import java.util.ArrayList;

public class OutgoingMessage extends QueuedMessage {
    
    private String serverId;    
    private String message;
    private String from;
    private String to;     
    
    private String localId;        
    private static int nextLocalId = 1;            
    
    
    
    public OutgoingMessage(App app)
    {
        super(app);
        this.localId = "_o" + getNextLocalId();
    }
    
    static synchronized int getNextLocalId()
    {
        return nextLocalId++;
    }
    
    public Uri getUri()
    {
        return Uri.withAppendedPath(App.OUTGOING_URI, ((serverId == null) ? localId : serverId));
    }
    
    public String getLogName()
    {
        return (serverId == null) ? "SMS reply" : ("SMS id=" + serverId);
    }
    
    public String getServerId()
    {
        return serverId;
    }
    
    public void setServerId(String id)
    {
        this.serverId = id;
    }    
           
    public String getMessageBody()
    {
        return message;
    }
    
    public void setMessageBody(String message)
    {
        this.message = message;
    }
    
    public String getFrom()
    {
        return from;
    }
    
    public void setFrom(String from)
    {
        this.from = from;
    }
    
    public String getTo()
    {
        return to;
    }
    
    public void setTo(String to)
    {
        this.to = to;
    }

    public void retryNow() {
        app.log("Retrying sending " + getLogName() + " to " + getTo());
        trySend();
    }
    
    public void trySend()
    {
        SmsManager smgr = SmsManager.getDefault();
        ArrayList<String> bodyParts = smgr.divideMessage(getMessageBody());
        
        int numParts = bodyParts.size();
        if (numParts > 1)
        {
            app.log("(Multipart message with "+numParts+" parts)");
        }
        
        String packageName = app.chooseOutgoingSmsPackage(bodyParts.size());
        
        if (packageName == null)
        {            
            // todo... schedule retry
            return;
        }
        
        Intent intent = new Intent(packageName + App.OUTGOING_SMS_INTENT_SUFFIX, this.getUri());
        intent.putExtra(App.OUTGOING_SMS_EXTRA_TO, getTo());
        intent.putExtra(App.OUTGOING_SMS_EXTRA_BODY, bodyParts);
        
        app.sendBroadcast(intent, "android.permission.SEND_SMS");
    }

    protected Intent getRetryIntent() {
        Intent intent = new Intent(app, OutgoingMessageRetry.class);
        intent.setData(this.getUri());
        return intent;
    }   
}
