CREATE PROCEDURE %%LOG-FTS-ERR-PROC%%(
 @p_id   NUMERIC,
 @p_code VARCHAR,
 @p_type VARCHAR,
 @p_msg  VARCHAR
) AS
BEGIN
 INSERT INTO %%ERR-TABLE%%(
  %%ERR-ID-COL%%,  %%ERR-CODE-COL%%,  %%ERR-TYPE-COL%%,%%ERR-MSG-COL%%
 ) VALUES(
  @p_id, @p_code, @p_type, SUBSTRING(@p_msg,1,%%ERR-MSG-MAX-LEN%%)
 )
END