package com.example.service_server;

interface AIDL_Service {
    void aidl_service();

    //传参时除了Java基本类型以及String，CharSequence之外的类型
    //都需要在前面加上定向tag，具体加什么量需而定
    // in:  data from client to server
    // out: data from server to client
    void aidl_setter(in int val);

    //所有的返回值前都不需要加任何东西，不管是什么数据类型
    int aidl_getter();
}
