CREATE PROCEDURE %%STAGE-RECORD-PROC%% (
 @id       NUMERIC,
 @args     VARCHAR(10),
 @err_code VARCHAR(255)
) AS
BEGIN

 DECLARE c CURSOR FOR 
  SELECT %%SRC-ID%%,%%SRC-FILENAME%% 
  FROM %%SRC-TABLE%% WHERE %%SRC-ID%%=@id
  
 DECLARE @fn            VARCHAR(255)
 DECLARE @tmpdir        VARCHAR(255) 
 DECLARE @filter        VARCHAR(255)
 DECLARE @cmd           VARCHAR(255)
 DECLARE @shell         INT
 DECLARE @fso           INT
 DECLARE @rc            INT
 DECLARE @count         INT
 DECLARE @deltempf      BIT
 DECLARE @printcmd      BIT
 DECLARE @err_type      VARCHAR(255)
 DECLARE @err_msg       VARCHAR(1024)
 DECLARE @ole_err_src   VARCHAR(255)
 DECLARE @ole_err_descr VARCHAR(255)
 
 SET NOCOUNT ON
 SET @err_type='%%ERRT-TXT-EXTR-FAIL%%'
 SET @deltempf = 1
 SET @printcmd = 0
 SET @count    = 0

 EXEC @rc=sp_OACreate 'WScript.Shell', @shell OUT
 IF @rc<>0 BEGIN
  EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, 
   'Error creating WScript.Shell OLE Automation object'
  DEALLOCATE c
  RETURN %%RC-OLE-ERR%%
 END

 EXEC @rc=sp_OACreate 'Scripting.FileSystemObject', @fso OUT
 IF @rc<>0 BEGIN
  EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, 
   'Error creating Scripting.FileSystemObject OLE Automation object'
  DEALLOCATE c
  EXEC sp_OADestroy @shell
  RETURN %%RC-OLE-ERR%%
 END
 
 EXEC @rc=sp_OAMethod @shell, 'ExpandEnvironmentStrings', @tmpdir OUT, '%%CONVERT-TMPDIR%%'
 IF @rc<>0 BEGIN
  SET @err_msg='OLE error using ExpandEnvironmentStrings method for WScript.Shell object'
  EXEC @rc=sp_OAGetErrorInfo @shell, @ole_err_src out, @ole_err_descr out
  IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
   + ' Description: ' + @ole_err_descr  
  EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
  DEALLOCATE c
  EXEC sp_OADestroy @shell
  EXEC sp_OADestroy @fso
  RETURN %%RC-OLE-ERR%%
 END
 
 EXEC @rc=sp_OAMethod @shell, 'ExpandEnvironmentStrings', @filter OUT, '%%CONVERT-EXE%%'
 IF @rc<>0 BEGIN
  SET @err_msg='OLE error using ExpandEnvironmentStrings method for WScript.Shell object'
  EXEC @rc=sp_OAGetErrorInfo @shell, @ole_err_src out, @ole_err_descr out
  IF @rc=0 SET @err_msg=@err_msg + '. Source: ' + @ole_err_src
   + ' Description: ' + @ole_err_descr  
  EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
  DEALLOCATE c
  EXEC sp_OADestroy @shell
  EXEC sp_OADestroy @fso
  RETURN %%RC-OLE-ERR%%
 END
  
 /* check we have a ifilter for ZIP files */
 DECLARE @ifilterfound INT
 EXEC @ifilterfound=%%IFILTER-DETECT-PROC%% @shell,'zip'
 IF @ifilterfound<>1 BEGIN
  SET @err_msg='Cant find IFilter for ZIP files'
  EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
  DEALLOCATE c
  EXEC sp_OADestroy @shell
  EXEC sp_OADestroy @fso
  RETURN %%RC-MISSING-ZIP-FILTER%%
 END
 
 OPEN c 
 FETCH c INTO @id,@fn
 WHILE (@@FETCH_STATUS=0) BEGIN
 
  SET @count=@count+1
  IF @count>1 BEGIN
   SET @err_msg='Too many rows in %%SRC-TABLE%% with such id (at least 2)'
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
   CLOSE C
   DEALLOCATE c
   EXEC sp_OADestroy @shell
   EXEC sp_OADestroy @fso
   RETURN %%RC-TOO-MANY-ROWS%%
  END

  DECLARE @pos INT, @c CHAR(1), @ext VARCHAR(255)
  IF @args='-z'
   SET @ext='txt'
  ELSE BEGIN 
   /* extract and check extension */
   
   SET @ext=''
   SET @pos=LEN(@fn)
   WHILE @pos > 0 BEGIN
    SET @c=SUBSTRING(@fn,@pos,1)
    IF @c='.' BREAK
    SET @ext=@c+@ext
    SET @pos=@pos-1
   END
   IF @ext IS NULL OR LEN(@ext)=0 OR LEN(@ext)=LEN(@fn) BEGIN
    SET @err_msg='Cannot determine file extension'
    EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
    CLOSE C
    DEALLOCATE c
    EXEC sp_OADestroy @shell
    EXEC sp_OADestroy @fso
    RETURN %%RC-BAD-FILE-EXT%%
   END
  
   /* check presence of IFilter */
   EXEC @ifilterfound=%%IFILTER-DETECT-PROC%% @shell,@ext
   IF @ifilterfound<>1 BEGIN
    SET @err_msg='Cant find IFilter for file extension: '+@ext
    EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
    CLOSE C
    DEALLOCATE c
    EXEC sp_OADestroy @shell
    EXEC sp_OADestroy @fso
    RETURN %%RC-MISSING-DOC-FILTER%%
   END
  END
  
  DECLARE @tmpbin VARCHAR(255)
  DECLARE @tmpzip VARCHAR(255)
  SET @tmpbin = @tmpdir+'\'+CONVERT(VARCHAR,@id)+'.'+@ext
  SET @tmpzip = @tmpdir+'\'+CONVERT(VARCHAR,@id)+'.zip'
  
  EXEC @rc = %%SAVE-LOB-TO-FILE%% @id, @tmpbin, @err_code, @err_type
  IF @rc<>1 BEGIN
   SET @err_msg='Save of BLOB to temp file failed'
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
   CLOSE C
   DEALLOCATE c
   EXEC sp_OADestroy @shell
   EXEC sp_OADestroy @fso
   RETURN %%RC-LOB-TO-FILE-ERR%%
  END
 
  SET @cmd=@filter+' '+@args +' '+@tmpbin+' '+@tmpzip
  IF @printcmd=1 PRINT 'cmd='+@cmd
  EXEC @rc=sp_OAMethod @shell, 'Run', NULL, @cmd, 2, 1 /* minimized, wait to exit */
  IF @rc<>0 BEGIN
   SET @err_msg='External filter/zip utility failed. Command was: '+@cmd 
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
   CLOSE C
   DEALLOCATE c
   EXEC sp_OADestroy @shell
   EXEC sp_OADestroy @fso
   RETURN %%RC-EXT-UTIL-ERR%%
  END

  /* update record in destination table */
  UPDATE %%DEST-TABLE%% SET %%DEST-EXT%%='zip',%%DEST-BLOB%%=0xFFFFFFFF WHERE %%DEST-ID%%=@id
  IF @@ERROR<>0 OR @@ROWCOUNT<>1 BEGIN
   IF @@ERROR<>0 
    SET @err_msg = 'Error updating table %%DEST-TABLE%% (error code '
     + CONVERT(VARCHAR,@@ERROR) + ')' 
   ELSE
    SET @err_msg = 'Error updating table %%DEST-TABLE%% - rows updated <> 1 ('
     + CONVERT(VARCHAR,@@ROWCOUNT) + ')' 
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
   CLOSE C
   DEALLOCATE c
   EXEC sp_OADestroy @shell
   EXEC sp_OADestroy @fso
   RETURN %%RC-UPDATE-ERR%%
  END

  EXEC @rc=%%READ-LOB-FROM-FILE%% @id, @tmpzip, @err_code, @err_type
  IF @rc<>1 BEGIN
   SET @err_msg='Load of BLOB from temp file failed' 
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
   CLOSE C
   DEALLOCATE c
   EXEC sp_OADestroy @shell
   EXEC sp_OADestroy @fso
   RETURN %%RC-FILE-TO-LOB-ERR%%
  END
  
  /* clean files */
  IF @deltempf=1 BEGIN
   EXEC sp_OAMethod @fso, 'DeleteFile', NULL, @tmpzip, 1
   EXEC sp_OAMethod @fso, 'DeleteFile', NULL, @tmpbin, 1
  END

  DELETE FROM %%SRC-TABLE%% WHERE %%SRC-ID%%=@id
  IF @@ERROR<>0 OR @@ROWCOUNT<>1 BEGIN
   IF @@ERROR<>0 
    SET @err_msg = 'Error deleting from table %%SRC-TABLE%% (error code '
     + CONVERT(VARCHAR,@@ERROR) + ')' 
   ELSE
    SET @err_msg = 'Error deleting from table %%SRC-TABLE%% - rows deleted <> 1 ('
     + CONVERT(VARCHAR,@@ROWCOUNT) + ')' 
   EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type,@err_msg
   CLOSE C
   DEALLOCATE c
   EXEC sp_OADestroy @shell
   EXEC sp_OADestroy @fso
   RETURN %%RC-DELETE-ERR%%
  END

  SET @count=@count+1
  FETCH NEXT FROM c INTO @id,@fn

 END
 
 CLOSE C
 DEALLOCATE C
 
 IF @count=0 BEGIN
  SET @err_msg='No rows to process in %%SRC-TABLE%%' 
  EXEC %%LOG-FTS-ERR-PROC%% @id, @err_code, @err_type, @err_msg
  RETURN %%RC-NO-ROWS%%
 END 
 
 RETURN %%RC-OK%% 
END
