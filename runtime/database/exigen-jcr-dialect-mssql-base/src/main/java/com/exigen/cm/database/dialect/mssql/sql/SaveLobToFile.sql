CREATE PROCEDURE %%SAVE-LOB-TO-FILE%% (
 @id INT,
 @filename VARCHAR(255),
 @err_code VARCHAR(255),
 @err_type VARCHAR(255) 
) AS
BEGIN
  DECLARE @bin AS INT
  DECLARE @rc AS INT
  DECLARE @chunksz INT
  DECLARE @chunk VARBINARY(4096)
  DECLARE @bytesrem INT
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

  SELECT @bytesrem=DATALENGTH(%%SRC-BLOB%%)
   FROM %%SRC-TABLE%%
   WHERE %%SRC-ID%%=@id
  IF @@ERROR<>0 BEGIN
   SET @err_msg='Error geting BLOB size from table %%SRC-TABLE%% (error code '
     + CONVERT(VARCHAR,@@ERROR) + ')'  
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg  
   EXEC sp_OADestroy @bin
   RETURN 0
  END
   
  SET @pos=1
  SET @chunksz=4096

  WHILE @bytesrem>0 BEGIN

   IF @bytesrem < 4096
    SET @chunksz=@bytesrem

   SELECT @chunk=SUBSTRING(%%SRC-BLOB%%,@pos,@chunksz)
    FROM %%SRC-TABLE%%
    WHERE %%SRC-ID%%=@id
   IF @@ERROR<>0 OR @@ROWCOUNT<>1 BEGIN
    IF @@ERROR<>0 
     SET @err_msg='Error selecting from %%SRC-TABLE%% (error code '
     + CONVERT(VARCHAR,@@ERROR) + ')' 
    ELSE
     SET @err_msg='Error selecting from %%SRC-TABLE%% - rows returned <> 1 ('
     + CONVERT(VARCHAR,@@ROWCOUNT) + ')' 
    EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
    EXEC sp_OADestroy @bin
    RETURN 0
   END

   EXEC @rc=sp_OAMethod @bin, "Write", NULL, @chunk
   IF @rc<>0 BEGIN
    SET @err_msg='OLE error calling Write method for ADODB.Stream object'
    EXEC @rc=sp_OAGetErrorInfo @bin, @ole_err_src out, @ole_err_descr out
    IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
     + ' Description: ' + @ole_err_descr  
    EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
    EXEC sp_OADestroy @bin
    RETURN 0
   END

   SET @pos=@pos+@chunksz
   SET @bytesrem=@bytesrem-@chunksz

  END

  EXEC @rc=sp_OAMethod @bin, "SaveToFile", NULL, @filename, 2 /* =adSaveCreateOverWrite */
  IF @rc<>0 BEGIN
    SET @err_msg='OLE error calling SaveToFile method for ADODB.Stream object'
    EXEC @rc=sp_OAGetErrorInfo @bin, @ole_err_src out, @ole_err_descr out
    IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
     + ' Description: ' + @ole_err_descr  
    EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
    EXEC sp_OADestroy @bin
    RETURN 0
  END

  EXEC sp_OADestroy @bin
  RETURN 1

END