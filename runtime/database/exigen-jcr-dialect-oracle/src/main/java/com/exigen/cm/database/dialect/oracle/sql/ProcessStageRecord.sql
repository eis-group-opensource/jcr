CREATE FUNCTION %%PROCESS-STAGE-RECORD-PROC%%(
 p_id IN NUMBER,
 p_errcode IN VARCHAR2,
 p_ziponly IN INTEGER
)RETURN NUMBER AS
 dst       CLOB;
 dstzip    BLOB;
 i         INTEGER :=0;
 v_errm    VARCHAR2(1000);
 v_errtype VARCHAR2(1000) := '%%ERRT-TXT-EXTR-FAIL%%';
BEGIN
 FOR x in (SELECT %%SRC-ID%%,%%SRC-BLOB%% FROM %%SRC-TABLE%% WHERE %%SRC-ID%%=p_id) LOOP
  i:=i+1;

  IF (i>1) THEN
   BEGIN
    %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
     'Too many records found with same id in %%SRC-TABLE%% (at least 2)'
    );
    RETURN %%RC-TOO-MANY-ROWS%%;
   END; 
  END IF;

  BEGIN
   DBMS_LOB.CREATETEMPORARY(dstzip, true);
   EXCEPTION WHEN OTHERS THEN BEGIN
     v_errm:=SQLERRM;
     %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
      'Creating temp BLOB for compressed text: '||v_errm);
     RETURN %%RC-EXTRACT-ERR%%;
   END;
  END;  

  IF (p_ziponly=0) THEN
   BEGIN
    DBMS_LOB.CREATETEMPORARY(dst, true);
    %%JCR_FTS_UTL_PKG%%.extract(x.%%SRC-BLOB%%,dst,x.%%SRC-ID%%);
    EXCEPTION WHEN OTHERS THEN BEGIN
     v_errm:=SQLERRM;
     %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
      'Extracting text from doc: '||v_errm);
     RETURN %%RC-EXTRACT-ERR%%;
    END;  
   END;
  ELSE
   BEGIN
    dst:=%%JCR_FTS_UTL_PKG%%.utf8txtblob_to_clob(x.%%SRC-BLOB%%);
    EXCEPTION WHEN OTHERS THEN
     BEGIN
      v_errm:=SQLERRM;
      %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
       'Converting BLOB with UTF8 text to CLOB: '||v_errm);
      RETURN %%RC-EXTRACT-ERR%%;
     END; 
   END; 
  END IF; 
  
  BEGIN
   IF DBMS_LOB.GETLENGTH(dst)=0 THEN
    DBMS_LOB.WRITE(dst,1,1,' ');
    dst:=' ';
   END IF;
   EXCEPTION WHEN OTHERS THEN BEGIN
    v_errm:=SQLERRM;
    %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
     'Check if text CLOB is empty fails: '||v_errm);
    RETURN %%RC-COMPRESS-ERR%%;
   END;
  END; 
    
  BEGIN
   %%JCR_FTS_UTL_PKG%%.pack(dst,dstzip);
   EXCEPTION WHEN OTHERS THEN BEGIN
    v_errm:=SQLERRM;
    %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
     'Compressing text CLOB to BLOB: '||v_errm);
    RETURN %%RC-COMPRESS-ERR%%;
   END; 
  END; 

  BEGIN
   UPDATE %%DEST-TABLE%% SET %%DEST-BLOB%%=dstzip
    WHERE %%DEST-ID%%=x.%%SRC-ID%%;
   EXCEPTION WHEN OTHERS THEN BEGIN
    v_errm:=SQLERRM;
     %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
      'Updating %%DEST-TABLE%%: '||v_errm);
     RETURN %%RC-UPDATE-ERR%%;
    END; 
  END;
    
  IF SQL%ROWCOUNT<>1 THEN
   BEGIN
    %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
     'UPDATE of %%DEST-TABLE%% reports num of updated records <> 1 ('||
       SQL%ROWCOUNT||')'
    );
    RETURN %%RC-UPDATE-ERR%%;
   END; 
  END IF; 

  BEGIN
   DBMS_LOB.FREETEMPORARY(dst);
   DBMS_LOB.FREETEMPORARY(dstzip);
   EXCEPTION WHEN OTHERS THEN BEGIN
    v_errm:=SQLERRM;
    %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
     'Deallocating  temp LOBs: '||v_errm);
   END;
  END;
  
  BEGIN
   DELETE FROM %%SRC-TABLE%% WHERE %%SRC-ID%%=p_id;
    EXCEPTION WHEN OTHERS THEN BEGIN
     v_errm:=SQLERRM;
     %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
      'Error deleting from %%SRC-TABLE%%: '||v_errm);
     RETURN %%RC-DELETE-ERR%%;
    END; 
  END; 
  
  IF SQL%ROWCOUNT<>1 THEN
   BEGIN
     %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
      'DELETE from %%SRC-TABLE%% reports num of deleted records <>1 ('||
         SQL%ROWCOUNT||')'
     );
    RETURN %%RC-DELETE-ERR%%;
   END; 
  END IF;

 END LOOP;
 
 IF i=0 THEN
  BEGIN
   %%LOG-FTS-ERR-PROC%%(p_id, p_errcode, v_errtype,
     'There is no records to process with such %%SRC-ID%% in table %%SRC-TABLE%%'
   );
   RETURN %%RC-NO-ROWS%%;
  END; 
 END IF;
 
 RETURN %%RC-OK%%;
END;