CREATE PROCEDURE %%READ-LOB-FROM-FILE%% (
 @id INT, @filename VARCHAR(255),
 @err_code VARCHAR(255),
 @err_type VARCHAR(255)
) AS
BEGIN
  DECLARE @bin AS INT
  DECLARE @rc AS INT
  DECLARE @chunksz INT
  DECLARE @chunk VARBINARY(4096)
  DECLARE @bytesrem INT
  DECLARE @eof BIT
  DECLARE @ptr VARBINARY(16)
  DECLARE @pos INT
  DECLARE @ole_err_src VARCHAR(255)
  DECLARE @ole_err_descr VARCHAR(255)
  DECLARE @err_msg VARCHAR(1024)
  
  EXEC @rc=sp_OACreate "ADODB.Stream", @bin out
  IF @rc<>0 BEGIN
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, 
    'Error creating ADODB.Stream OLE Automation object'
   RETURN 0
  END 

  EXEC @rc=sp_OASetProperty @bin, "Type",1 /* binary file=1 text file=2 */
  IF @rc<>0 BEGIN
   SET @err_msg='OLE error setting Type property for ADODB.Stream object'
   EXEC @rc=sp_OAGetErrorInfo @bin, @ole_err_src out, @ole_err_descr out
   IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
    + ' Description: ' + @ole_err_descr  
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
   EXEC sp_OADestroy @bin
   RETURN 0
  END 

  EXEC @rc=sp_OAMethod @bin, "Open"
  IF @rc<>0 BEGIN
   SET @err_msg='OLE error calling Open method for ADODB.Stream object'
   EXEC @rc=sp_OAGetErrorInfo @bin, @ole_err_src out, @ole_err_descr out
   IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
    + ' Description: ' + @ole_err_descr  
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
   EXEC sp_OADestroy @bin
   RETURN 0
  END

  EXEC @rc=sp_OAMethod @bin, "LoadFromFile", NULL, @filename
  IF @rc<>0 BEGIN
   SET @err_msg='OLE error calling LoadFromFile method for ADODB.Stream object'
   EXEC @rc=sp_OAGetErrorInfo @bin, @ole_err_src out, @ole_err_descr out
   IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
    + ' Description: ' + @ole_err_descr  
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
   EXEC sp_OADestroy @bin
   RETURN 0
  END 

  SELECT @ptr=TEXTPTR(%%DEST-BLOB%%) FROM %%DEST-TABLE%% WHERE %%DEST-ID%%=@id
  IF @@ERROR<>0 OR @@ROWCOUNT<>1 BEGIN
   IF @@ERROR<>0 
    SET @err_msg=
     'Error geting text pointer from table %%DEST-TABLE%% (error code '
     + CONVERT(VARCHAR,@@ERROR) + ')' 
   ELSE
    SET @err_msg=
     'Error getting text pointer from table %%DEST-TABLE%% - rows returned <> 1 ('
     + CONVERT(VARCHAR,@@ROWCOUNT) + ')' 
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
   EXEC sp_OADestroy @bin
   RETURN 0
  END

  UPDATETEXT %%DEST-TABLE%%.%%DEST-BLOB%% @ptr 0 4
  IF @@ERROR<>0 BEGIN
   SET @err_msg='Error updating BLOB in %%DEST-TABLE%% (error code '
     + CONVERT(VARCHAR,@@ERROR) + ')'
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg  
   EXEC sp_OADestroy @bin
   RETURN 0
  END

  SET @chunksz=4096
  SET @pos=0

  EXEC @rc=sp_OAGetProperty @bin, "EOS", @eof OUT
  IF @rc<>0 BEGIN
   SET @err_msg='OLE error getting EOS property from ADODB.Stream object (before loop)'
   EXEC @rc=sp_OAGetErrorInfo @bin, @ole_err_src out, @ole_err_descr out
   IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
    + ' Description: ' + @ole_err_descr  
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
   EXEC sp_OADestroy @bin
   RETURN 0
  END

  WHILE @eof=0 BEGIN

   EXEC @rc=sp_OAMethod @bin, "Read", @chunk OUT, @chunksz
   IF @rc<>0 BEGIN
    SET @err_msg='OLE error using Read method for ADODB.Stream object'
    EXEC @rc=sp_OAGetErrorInfo @bin, @ole_err_src out, @ole_err_descr out
    IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
     + ' Description: ' + @ole_err_descr  
    EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
    EXEC sp_OADestroy @bin
    RETURN 0
   END

   UPDATETEXT %%DEST-TABLE%%.%%DEST-BLOB%% @ptr @pos 0 @chunk
   IF @@ERROR<>0 BEGIN
    SET @err_msg='Error using UPDATETEXT on %%DEST-TABLE%% (error code '
     + CONVERT(VARCHAR,@@ERROR) + ')'
    EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg  
    EXEC sp_OADestroy @bin
    RETURN 0
   END

   SET @pos=@pos+DATALENGTH(@chunk)

   EXEC @rc=sp_OAGetProperty @bin, "EOS", @eof OUT
   IF @rc<>0 BEGIN
    SET @err_msg='OLE error getting EOS property from ADODB.Stream object (within loop)'
    EXEC @rc=sp_OAGetErrorInfo @bin, @ole_err_src out, @ole_err_descr out
    IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
     + ' Description: ' + @ole_err_descr  
    EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
    EXEC sp_OADestroy @bin
    RETURN 0
   END
  END

  EXEC sp_OADestroy @bin
  
  UPDATE %%DEST-TABLE%% SET %%DEST-BLOB%%=%%DEST-BLOB%% WHERE %%DEST-ID%%=@id
  IF @@ERROR<>0 OR @@ROWCOUNT<>1 BEGIN
   IF @@ERROR<>0 
    SET @err_msg='Error updating %%DEST-TABLE%% (error code '
     + CONVERT(VARCHAR,@@ERROR) + ')' 
   ELSE
    SET @err_msg='Error updating %%DEST-TABLE%% - rows updated <> 1 ('
     + CONVERT(VARCHAR,@@ROWCOUNT) + ')' 
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
   RETURN 0
  END 
  
  RETURN 1

END