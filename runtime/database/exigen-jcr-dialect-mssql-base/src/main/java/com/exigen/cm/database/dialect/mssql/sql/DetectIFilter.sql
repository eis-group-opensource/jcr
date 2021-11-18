CREATE   PROCEDURE %%IFILTER-DETECT-PROC%%(@shell INT, @ext VARCHAR(255)) AS
BEGIN

/*

    * Step 1: Determine whether there is a PersistentHandler associated with the file extension.
      This can be found in the Registry under HKEY_LOCAL_MACHINE\Software\Classes\FileExtension;
      for example, HKLM\Software\Classes\.htm. The default value of the sub key called
      PersistentHandler gives you the GUID of the PersistenHandler. If present, skip to Step Four;
      otherwise, continue with Step Two.

    * Step 2: Determine the CLSID associated with the file extension. Take the default value that
      is associated with the extension; for example, "htmlfile" for the key
      HKLM\Software\Classes\.htm. Next, search for that entry—for example,
      "hmtlfile"—under HKLM\ Software\Classes. The default value of the sub key CLSID contains the
      CLSID associated with that file extension.

    * Step 3: Next, search for that CLSID under HKLM\Software\Classes\CLSID. The default value of
      the sub key called PersistentHandler gives you the GUID of the PersistenHandler.

    * Step 4: Search for that GUID under HKLM\Software\Classes\CLSID. Under it, you find a
      PersistentAddinsRegistered sub key that has always a {89BCB740-6119-101A-BCB7-00DD010655AF}
      sub key (this is the GUID of the IFilter interface). The default value of this key has the
      IFilter PersistenHandler GUID.

    * Step 5: Search for this GUID once more under HKLM\Software\Classes\CLSID. Under its key, you
      find the InProcServer32 sub key and its default value contains the name of the DLL that provides
      the IFilter interface to use for this extension. For example, for the .htm and .html extension,
      this is the nlhtml.dll DLL.

      http://www.codeguru.com/csharp/csharp/cs_internet/desktopapplications/article.php/c10245/

*/

 DECLARE @rc INT
 DECLARE @key VARCHAR(1000)
 DECLARE @val VARCHAR(1000)
 DECLARE @printval BIT
 SET @printval=0

 /* step 1 */
 IF @printval=1 PRINT 'Ext='+@ext+' Step1...'
 SET @key='HKLM\Software\Classes\.'+@ext+'\PersistentHandler\'
 EXEC @rc=sp_OAMethod @shell, 'RegRead', @val OUT, @key
 IF @rc=0 BEGIN 
  IF @printval=1 PRINT 'Ext='+@ext+' Step1 val='+@val
 END

 ELSE BEGIN

  /* step 2a */
  IF @printval=1 PRINT 'Ext='+@ext+' Step2a...'
  SET @key='HKLM\Software\Classes\.'+@ext+'\'
  EXEC @rc=sp_OAMethod @shell, 'RegRead', @val OUT, @key
  IF @rc<>0 RETURN 0
  IF @printval=1 PRINT 'Ext='+@ext+' Step2a val='+@val

  /* step 2b */
  IF @printval=1 PRINT 'Ext='+@ext+' Step2b...'
  SET @key='HKLM\Software\Classes\'+@val+'\CLSID\'
  EXEC @rc=sp_OAMethod @shell, 'RegRead', @val OUT, @key
  IF @rc<>0 RETURN 0
  IF @printval=1 PRINT 'Ext='+@ext+' Step2b val='+@val

  /* step 3 */
  IF @printval=1 PRINT 'Ext='+@ext+' Step3...'
  SET @key='HKLM\Software\Classes\CLSID\'+@val+'\PersistentHandler\'
  EXEC @rc=sp_OAMethod @shell, 'RegRead', @val OUT, @key
  IF @rc<>0 RETURN 0
  IF @printval=1 PRINT 'Ext='+@ext+' Step3 val='+@val
 END

 /* step 4 - after step 1 or step 3*/
 IF @printval=1 PRINT 'Ext='+@ext+' Step4...'
 SET @key='HKLM\Software\Classes\CLSID\'+@val+'\PersistentAddinsRegistered\{89BCB740-6119-101A-BCB7-00DD010655AF}\' 
 EXEC @rc=sp_OAMethod @shell, 'RegRead', @val OUT, @key
 IF @rc<>0 RETURN 0
 IF @printval=1 PRINT 'Ext='+@ext+' Step4 val='+@val

 /* step 5 */
 IF @printval=1 PRINT 'Ext='+@ext+' Step5...'
 SET @key='HKLM\Software\Classes\CLSID\'+@val+'\InprocServer32\'
 EXEC @rc=sp_OAMethod @shell, 'RegRead', @val OUT, @key
 IF @rc<>0 RETURN 0
 IF @printval=1 PRINT 'Ext='+@ext+' Step5 val='+@val
 IF @val IS NULL OR LEN(@val)=0 RETURN 0
 RETURN 1
END
