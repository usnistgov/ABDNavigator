"../ABDNavigator/jdk/bin/javac" -cp lib/* -d bin src/bot/*.java src/com/*.java src/controllers/lithoRasters/*.java src/controllers/*.java src/gds/*.java src/gui/*.java src/main/*.java
cd bin
"../ABDNavigator/jdk/bin/javac" -h com.MatrixInterface
cd ..
copy bin\com_MatrixInterface.h src\com_MatrixInterface.h
copy bin\com_MatrixInterface.h ..\ABDController_C_Code\vc\com_MatrixInterface\com_MatrixInterface\com_MatrixInterface.h
