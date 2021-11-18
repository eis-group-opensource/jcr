CREATE PACKAGE %%JCR_FTS_UTL_PKG%% AS
 PROCEDURE pack   (p_src IN clob, p_dst IN OUT blob);
 PROCEDURE pack   (p_src IN blob, p_dst IN OUT blob);
 PROCEDURE unpack (p_src IN blob, p_dst IN OUT clob);
 FUNCTION utf8txtblob_to_clob (p_src IN BLOB) RETURN CLOB;
 PROCEDURE extract(p_src IN blob, p_dst IN OUT clob, p_id IN NUMBER);
END;
/

CREATE PACKAGE BODY %%JCR_FTS_UTL_PKG%% AS
 PROCEDURE pack(p_src IN clob,p_dst IN OUT blob) IS
  tmp      BLOB;
  ofs1     INTEGER := 1;
  ofs2     INTEGER := 1;
  csid     NUMBER  := DBMS_LOB.DEFAULT_CSID;
  lngctx   INTEGER := DBMS_LOB.DEFAULT_LANG_CTX;
  w        INTEGER;
 BEGIN
  DBMS_LOB.CREATETEMPORARY(tmp,TRUE);
  DBMS_LOB.CONVERTTOBLOB(tmp,p_src,DBMS_LOB.LOBMAXSIZE,ofs1,ofs2,csid,lngctx,w);
  p_dst:=UTL_COMPRESS.LZ_COMPRESS(tmp,9);
  DBMS_LOB.FREETEMPORARY(tmp);
 END;
 PROCEDURE pack   (p_src IN blob, p_dst IN OUT blob) IS
 BEGIN
  p_dst:=UTL_COMPRESS.LZ_COMPRESS(p_src,9);
 END;
 PROCEDURE unpack(p_src IN blob, p_dst IN OUT clob) IS
  tmp      BLOB;
  ofs1     INTEGER := 1;
  ofs2     INTEGER := 1;
  csid     NUMBER  := DBMS_LOB.DEFAULT_CSID;
  lngctx   INTEGER := DBMS_LOB.DEFAULT_LANG_CTX;
  w        INTEGER;
 BEGIN
  tmp:=UTL_COMPRESS.LZ_UNCOMPRESS(p_src);
  DBMS_LOB.CONVERTTOCLOB(p_dst,tmp,DBMS_LOB.LOBMAXSIZE,ofs1,ofs2,csid,lngctx,w);
  DBMS_LOB.FREETEMPORARY(tmp);
 END;
 PROCEDURE extract(p_src IN BLOB, p_dst IN OUT clob,p_id IN NUMBER) IS
 BEGIN
  CTX_DOC.POLICY_FILTER('%%FMT-DOC-POLICY%%',p_src,p_dst,TRUE);
 END;
 FUNCTION utf8txtblob_to_clob (p_src IN BLOB) RETURN CLOB IS
  tmp CLOB;
  ofs1     INTEGER := 1;
  ofs2     INTEGER := 1;
  csid     NUMBER  := 871;
  lngctx   INTEGER := DBMS_LOB.DEFAULT_LANG_CTX;
  w        INTEGER;
 BEGIN
  DBMS_LOB.CREATETEMPORARY(tmp,TRUE);
  DBMS_LOB.CONVERTTOCLOB(tmp,p_src,DBMS_LOB.LOBMAXSIZE,ofs1,ofs2,csid,lngctx,w);
  RETURN tmp;
 END;
END;
/