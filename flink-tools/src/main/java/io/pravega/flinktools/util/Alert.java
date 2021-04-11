package io.pravega.flinktools.util;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Alert implements Serializable {
    public String AlertDescription;
    public String poNumber;
    public String status;
    public String date;

    @Override
    public String toString() {
        return "Alert{" +
                "AlertDescription='" + AlertDescription + '\'' +
                ", poNumber='" + poNumber + '\'' +
                ", status='" + status + '\'' +
                ", date=" + date +
                '}';
    }
}
