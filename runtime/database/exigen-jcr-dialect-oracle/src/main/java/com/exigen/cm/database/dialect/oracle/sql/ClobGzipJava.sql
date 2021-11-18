DECLARE
 stmt VARCHAR2(32000);
 CURSOR c1 IS select status from all_objects where object_name='DBMS_JAVA' and owner='SYS' and object_type='PACKAGE';
 CURSOR c2 IS select count(*) c from all_objects where object_type like '%JAVA%';
 flag1  BOOLEAN := FALSE;
 flag2  BOOLEAN := FALSE;
 x1     c1%ROWTYPE;
 x2     c2%ROWTYPE;
BEGIN
 FOR x1 IN c1 LOOP
  IF x1.status='VALID' THEN
   flag1:=TRUE;
  ELSE
   RAISE_APPLICATION_ERROR(-20000,'Check JavaVM installation (DBMS_JAVA status <> VALID)'); 
  END IF; 
 END LOOP;
 FOR x2 IN c2 LOOP
  IF x2.c>8000 THEN
   flag2:=TRUE;
  ELSE
   RAISE_APPLICATION_ERROR(-20000,
    'Check JavaVM installation (number of JAVA objects in dictionary is small: '||
     x2.c||')'); 
  END IF; 
 END LOOP;
 IF (NOT flag1) OR (NOT flag2) THEN
  RAISE_APPLICATION_ERROR(-20000,'For Oracle version 9 JavaVM required');
 END IF;
 stmt := 'CREATE JAVA SOURCE NAMED %%CLOBGZIP-JAVA%% AS';
 stmt := stmt ||CHR(10)|| 'import java.io.*;';
 stmt := stmt ||CHR(10)|| 'import java.util.zip.*;';
 stmt := stmt ||CHR(10)|| 'import java.sql.*;';
 stmt := stmt ||CHR(10)|| 'import oracle.sql.*;';
 stmt := stmt ||CHR(10)|| 'public class %%CLOBGZIP-JAVA%%';
 stmt := stmt ||CHR(10)|| '{';
 stmt := stmt ||CHR(10)|| '  public static void packclob(oracle.sql.CLOB srcClob, oracle.sql.BLOB dstBlob[]) {';
 stmt := stmt ||CHR(10)|| '    try {';
 stmt := stmt ||CHR(10)|| '      OutputStream outBuffer = dstBlob[0].getBinaryOutputStream();';
 stmt := stmt ||CHR(10)|| '      Reader inBuffer = srcClob.getCharacterStream();';
 stmt := stmt ||CHR(10)|| '      GZIPOutputStream gzip = new GZIPOutputStream(outBuffer);';
 stmt := stmt ||CHR(10)|| '      char[] tmpBuffer = new char[256];';
 stmt := stmt ||CHR(10)|| '      int n;';
 stmt := stmt ||CHR(10)|| '      while ((n = inBuffer.read(tmpBuffer)) >= 0){';
 stmt := stmt ||CHR(10)|| '        String s = new String(tmpBuffer);';
 stmt := stmt ||CHR(10)|| '        byte[] x=s.getBytes();';
 stmt := stmt ||CHR(10)|| '        gzip.write(x, 0, x.length);';
 stmt := stmt ||CHR(10)|| '      }';
 stmt := stmt ||CHR(10)|| '      gzip.close();';
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '    catch (SQLException e) {';
 stmt := stmt ||CHR(10)|| '      System.err.println(e);';     
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '    catch (IOException e) {';
 stmt := stmt ||CHR(10)|| '      System.err.println(e);';     
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '  }';
 stmt := stmt ||CHR(10)|| '  public static void packblob(oracle.sql.BLOB srcBlob, oracle.sql.BLOB dstBlob[]) {';
 stmt := stmt ||CHR(10)|| '    try {';
 stmt := stmt ||CHR(10)|| '      OutputStream outBuffer = dstBlob[0].getBinaryOutputStream();';
 stmt := stmt ||CHR(10)|| '      InputStream inBuffer = srcBlob.getBinaryStream();';
 stmt := stmt ||CHR(10)|| '      GZIPOutputStream gzip = new GZIPOutputStream(outBuffer);';
 stmt := stmt ||CHR(10)|| '      byte[] tmpBuffer = new byte[256];';
 stmt := stmt ||CHR(10)|| '      int n;';
 stmt := stmt ||CHR(10)|| '      while ((n = inBuffer.read(tmpBuffer)) >= 0){';
 stmt := stmt ||CHR(10)|| '        gzip.write(tmpBuffer, 0, tmpBuffer.length);';
 stmt := stmt ||CHR(10)|| '      }';
 stmt := stmt ||CHR(10)|| '      gzip.close();';
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '    catch (SQLException e) {';
 stmt := stmt ||CHR(10)|| '      System.err.println(e);';     
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '    catch (IOException e) {';
 stmt := stmt ||CHR(10)|| '      System.err.println(e);';     
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '  }';
 stmt := stmt ||CHR(10)|| '  public static void unpack(oracle.sql.BLOB srcBlob, oracle.sql.CLOB dstClob[]) {';
 stmt := stmt ||CHR(10)|| '    try {';
 stmt := stmt ||CHR(10)|| '      Writer outBuffer = dstClob[0].getCharacterOutputStream();';
 stmt := stmt ||CHR(10)|| '      InputStream inBuffer = srcBlob.getBinaryStream();';
 stmt := stmt ||CHR(10)|| '      GZIPInputStream gzip = new GZIPInputStream(inBuffer);';
 stmt := stmt ||CHR(10)|| '      InputStreamReader isr = new InputStreamReader(gzip);';
 stmt := stmt ||CHR(10)|| '      char[] tmpBuffer = new char[256];';
 stmt := stmt ||CHR(10)|| '      int n;';
 stmt := stmt ||CHR(10)|| '      while ((n = isr.read(tmpBuffer)) >= 0)';
 stmt := stmt ||CHR(10)|| '        outBuffer.write(tmpBuffer, 0, n);';
 stmt := stmt ||CHR(10)|| '      outBuffer.close();';
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '    catch (SQLException e) {';
 stmt := stmt ||CHR(10)|| '      System.err.println(e);';   
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '    catch (IOException e) {';
 stmt := stmt ||CHR(10)|| '      System.err.println(e);';     
 stmt := stmt ||CHR(10)|| '    }';
 stmt := stmt ||CHR(10)|| '  }';
 stmt := stmt ||CHR(10)|| '};';
 EXECUTE IMMEDIATE stmt;
 stmt := 'ALTER JAVA SOURCE %%CLOBGZIP-JAVA%% COMPILE';
 EXECUTE IMMEDIATE stmt;
END;