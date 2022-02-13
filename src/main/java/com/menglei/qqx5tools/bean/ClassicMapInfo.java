package com.menglei.qqx5tools.bean;

import com.menglei.qqx5tools.SettingsAndUtils.QQX5MapType;

import java.io.File;

/**
 * @author MengLeiFudge
 */
public class ClassicMapInfo extends QQX5MapInfo {
    public ClassicMapInfo(File xml) {
        super(xml, QQX5MapType.CLASSIC);
    }

    @Override
    public void setBasicInfo() {

    }

    @Override
    public void setDescribe() {

    }
}
