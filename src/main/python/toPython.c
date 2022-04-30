#include <Python.h>

int main( void )
{
    //todo: 이 파일에서 yt_dlp_kt.py 함수 실행할 수 있게 할 것. 직접 호출시 안 됨.
    Py_Initialize();
    PyRun_SimpleString("import yt_dlp_kt");
    PyRun_SimpleString("print( callme.test(\"test\") ) ");
    Py_Finalize();
    return 0;
}