package com.arpit.androidwebrtclient

class IceServerModel {

    private var urls: ArrayList<String?>? = null
    private var username: String? = null
    private var credential: String? = null

    fun getUrls(): ArrayList<String?>? {
        return urls
    }

    fun setUrls(urls: ArrayList<String?>?) {
        this.urls = urls
    }

    fun getUsername(): String? {
        return username
    }

    fun setUsername(username: String?) {
        this.username = username
    }

    fun getCredential(): String? {
        return credential
    }

    fun setCredential(credential: String?) {
        this.credential = credential
    }

    override fun toString(): String {
        return "IceServerModel{" +
                "urls=" + urls +
                ", username='" + username + '\'' +
                ", password='" + credential + '\'' +
                '}'
    }
}