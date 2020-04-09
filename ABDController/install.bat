javac -cp lib/* -d bin src/bot/*.java src/com/*.java src/controllers/lithoRasters/*.java src/controllers/*.java src/gds/*.java src/gui/*.java src/main/*.java
javah com.MatrixInterface
copy src\com_MatrixInterface.h bin\com_MatrixInterface.h
copy src\com_MatrixInterface.h ..\ABDController_C_Code\vc\com_MatrixInterface\com_MatrixInterface\com_MatrixInterface.h
