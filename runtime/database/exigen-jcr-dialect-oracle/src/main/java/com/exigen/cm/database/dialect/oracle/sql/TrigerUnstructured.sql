CREATE TRIGGER %%TRIGGER_NAME%% BEFORE INSERT
 OR UPDATE OF DOUBLE_VALUE,DATE_VALUE,BOOLEAN_VALUE,LONG_VALUE
 ON %%TABLE-UNSTRUCTURED-PROP%%
FOR EACH ROW
BEGIN
:new.STRING_VALUE:=CASE
    WHEN :new.DOUBLE_VALUE IS NOT NULL THEN concat('.',TO_CHAR(:new.DOUBLE_VALUE))
    WHEN :new.DATE_VALUE IS NOT NULL THEN concat('.',TO_CHAR(:new.DATE_VALUE))
    WHEN :new.BOOLEAN_VALUE IS NOT NULL THEN concat('.',TO_CHAR(:new.BOOLEAN_VALUE))
    WHEN :new.LONG_VALUE IS NOT NULL THEN concat('.',TO_CHAR(:new.LONG_VALUE))
    ELSE :new.STRING_VALUE END;
END;
