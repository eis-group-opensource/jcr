CREATE FUNCTION %%ZIP-PROC%% (p_id IN NUMBER) RETURN NUMBER AS
 dst CLOB;
 dstzip BLOB;
 i      INTEGER:=0;
BEGIN
 FOR x in (SELECT %%SRC-ID%%,%%SRC-BLOB%% FROM %%SRC-TABLE%% WHERE %%SRC-ID%%=p_id) LOOP
  i:=i+1;
  IF (i>1) THEN
   RETURN %%RC-TOO-MANY-ROWS%%;
  END IF;
  DBMS_LOB.CREATETEMPORARY(dstzip, true);
  BEGIN
   dst:=%%JCR_FTS_UTL_PKG%%.utf8txtblob_to_clob(x.%%SRC-BLOB%%);
   EXCEPTION WHEN OTHERS THEN RETURN %%RC-EXTRACT-ERR%%;
  END; 
  BEGIN 
   %%JCR_FTS_UTL_PKG%%.pack(dst,dstzip);
   EXCEPTION WHEN OTHERS THEN RETURN %%RC-COMPRESS-ERR%%;
  END; 
  BEGIN
   UPDATE %%DEST-TABLE%% SET %%DEST-BLOB%%=dstzip WHERE %%DEST-ID%%=x.%%SRC-ID%%;
   EXCEPTION WHEN OTHERS THEN RETURN %%RC-UPDATE-ERR%%;
  END;   
  IF SQL%ROWCOUNT<>1 THEN
   RETURN %%RC-UPDATE-ERR%%;
  END IF; 
  DBMS_LOB.FREETEMPORARY(dst);
  DBMS_LOB.FREETEMPORARY(dstzip);
  BEGIN
   DELETE FROM %%SRC-TABLE%%
    WHERE %%SRC-ID%%=p_id;
    EXCEPTION WHEN OTHERS THEN RETURN %%RC-DELETE-ERR%%;
  END; 
  IF SQL%ROWCOUNT<>1 THEN
   RETURN %%RC-DELETE-ERR%%;
  END IF;
 END LOOP;
 IF i=0 THEN
  RETURN %%RC-NO-ROWS%%;
 ELSE
  RETURN %%RC-OK%%;
 END IF;
END;