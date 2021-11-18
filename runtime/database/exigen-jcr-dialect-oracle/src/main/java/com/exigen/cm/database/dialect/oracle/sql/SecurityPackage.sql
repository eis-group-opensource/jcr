CREATE PACKAGE %%SECURITY-PKG%% AS
    FUNCTION pread(
    	p_node_id IN NUMBER,
        p_secid IN NUMBER,
        p_u IN VARCHAR2,
        p_glist IN VARCHAR2,
        p_ctxlist IN VARCHAR2, 
        p_allow_browse IN INTEGER
    ) RETURN INTEGER;
    FUNCTION premovetree(
        p_nodeid IN NUMBER,
        p_u IN VARCHAR2,
        p_glist IN VARCHAR2,
        p_ctxlist IN VARCHAR2
    ) RETURN INTEGER;
END %%SECURITY-PKG%%;
/

CREATE PACKAGE BODY %%SECURITY-PKG%% AS
    cached_glist VARCHAR2(32766) := '';
    cached_ctxlist VARCHAR2(32766) := '';
    cached_userid VARCHAR2(255)   := '';
    cached_allow_browse INTEGER         := -1;
    max_entries INTEGER         := 100;
    iteration INTEGER         :=0;
    TYPE TACERow IS RECORD(
        group_id cm_ace.group_id%type,
        user_id cm_ace.user_id%type,
        context_id cm_ace.context_id%type,
        isauth  cm_ace.p_read%type,
        p_x_super_deny cm_ace.p_x_super_deny%type,
        isauth_direct cm_ace.p_read_direct%type
    );
    TYPE TACESet IS REF CURSOR RETURN TACERow;
    TYPE TJCRSecCacheEntry IS RECORD (pread INTEGER,used INTEGER);
    TYPE TJCRSecCache IS TABLE OF TJCRSecCacheEntry INDEX BY BINARY_INTEGER;
    securityCache TJCRSecCache;

FUNCTION isauth(p_node_id_eq_sec_id IN BOOLEAN,aces TACESet,p_u IN VARCHAR2,p_glist IN VARCHAR2,p_ctxlist IN VARCHAR2) RETURN INTEGER IS
    contextUserAllow boolean := false;
    contextUserDeny boolean := false;
    contextGroupAllow boolean := false;
    contextGroupDeny boolean := false;
    userAllow boolean := false;
    userDeny boolean := false;
    groupAllow boolean := false;
    groupDeny boolean:= false;
    value char(1);
    ace TACERow; 
BEGIN
    LOOP
        FETCH aces INTO ace;
        EXIT WHEN aces%NOTFOUND;
        IF ace.p_x_super_deny IS NOT NULL AND ace.p_x_super_deny='1' THEN
            return 0;
        END IF;
        IF ace.isauth IS NULL THEN
            GOTO CONTINUE_LABEL;
        ELSE
            value:=ace.isauth;
        END IF;
        IF ace.isauth_direct IS NOT NULL AND ace.isauth_direct='1' THEN
        	IF NOT p_node_id_eq_sec_id THEN
        		GOTO CONTINUE_LABEL;
        	END IF;
        END IF;
        IF ace.group_id IS NOT NULL THEN
            IF ace.context_id IS NOT NULL THEN
                IF value='1' THEN
                    contextGroupAllow := true;
                ELSE
                    contextGroupDeny := true;
                END IF;
            ELSE
                IF value='1' THEN
                    groupAllow := true;
                ELSE
                    groupDeny := true;
                END IF;
            END IF;
            GOTO CONTINUE_LABEL;
        END IF;
        IF ace.user_id IS NOT NULL THEN
            IF ace.context_id IS NOT NULL THEN
                IF value='1' THEN
                    contextUserAllow := true;
                ELSE
                    contextUserDeny := true;
                    RETURN 0;
                END IF;
            ELSE
                IF value='1' THEN
                    userAllow := true;
                ELSE
                    userDeny := true;
                END IF;
            END IF;
        END IF;
        <<CONTINUE_LABEL>> NULL;
    END LOOP;
    IF contextUserDeny THEN
        RETURN 0;
    END IF;
    IF contextUserAllow AND NOT contextGroupDeny THEN
        RETURN 1;
    END IF;
    IF contextGroupDeny THEN
        RETURN 0;
    END IF;
    IF contextGroupAllow THEN
        RETURN 1;
    END IF;
    IF userDeny THEN
        RETURN 0;
    END IF;
    IF userAllow THEN
        RETURN 1;
    END IF;
    IF groupDeny THEN
        RETURN 0;
    END IF;
    IF groupAllow THEN
        RETURN 1;
    END IF;
    RETURN NULL;
END isauth; 

FUNCTION pread_(
	p_node_id IN NUMBER,
    p_secid IN NUMBER,
    p_u IN VARCHAR2,
    p_glist IN VARCHAR2,
    p_ctxlist IN VARCHAR2,
    p_allow_browse IN INTEGER
) RETURN INTEGER IS
    aces TACESet;
    e TJCRSecCacheEntry;
    node_id_eq_sec_id boolean;
BEGIN
    IF p_allow_browse=1 THEN    
        OPEN aces FOR
            SELECT group_id,user_id,context_id,
                CASE
                    WHEN p_read='1' OR p_browse='1' THEN '1'
                    WHEN p_read='0' OR p_browse='0' THEN '0'
                    ELSE NULL 
                END AS isauth , p_x_super_deny,
                CASE
                	WHEN p_read='1' THEN p_read_direct
                    WHEN p_browse='1' THEN p_browse_direct
                    WHEN p_read='0' THEN p_read_direct
                    WHEN p_browse='0' THEN p_browse_direct
                    ELSE NULL
                END AS isauth_direct
            FROM cm_ace 
            WHERE security_id=p_secid
                AND (user_id=p_u OR (
                    p_glist IS NOT NULL
                    AND group_id IS NOT NULL
                    AND INSTR(','||p_glist||',',','||group_id||',')>0 
                ))
                AND (context_id IS NULL OR (
                    p_ctxlist IS NOT NULL 
                    AND INSTR(','||p_ctxlist||',',','||context_id||',')>0
                ));
    ELSE
        OPEN aces FOR
            SELECT group_id,user_id,context_id,p_read AS isauth, p_x_super_deny, p_read_direct AS isauth_direct
            FROM cm_ace 
            WHERE security_id=p_secid
                AND (user_id=p_u OR (
                    p_glist IS NOT NULL 
                    AND group_id IS NOT NULL 
                    AND INSTR(','||p_glist||',',','||group_id||',')>0 
                ))
                AND (context_id IS NULL OR (
                    p_ctxlist IS NOT NULL 
                    AND INSTR(','||p_ctxlist||',',','||context_id||',')>0
                ));
    END IF;
    IF p_node_id=p_secid THEN
    	node_id_eq_sec_id:=TRUE;
    ELSE
    	node_id_eq_sec_id:=FALSE;
    END IF; 
    e.pread:=isauth(node_id_eq_sec_id,aces,p_u,p_glist,p_ctxlist);
    CLOSE aces;
    e.used:=iteration;
    IF (p_secid IS NOT NULL) AND (p_node_id IS NOT NULL) THEN
    	IF node_id_eq_sec_id THEN
    		securityCache(p_secid):=e;
    	ELSE
    		securityCache(-p_secid):=e;
    	END IF;
    END IF;	
    RETURN e.pread;
END pread_;

FUNCTION premovetree(
    p_nodeid IN NUMBER,
    p_u IN VARCHAR2,
    p_glist IN VARCHAR2,
    p_ctxlist IN VARCHAR2
) RETURN INTEGER IS
    sec_id cm_ace.security_id%TYPE;
    aces TACESet;
    rc   INTEGER;
    node_id_eq_sec_id BOOLEAN;
BEGIN
    IF p_u IS NULL OR p_nodeid IS NULL THEN
        RETURN 0;
    END IF;
    BEGIN
        SELECT security_id INTO sec_id FROM cm_node WHERE id=p_nodeid;
        EXCEPTION
            WHEN NO_DATA_FOUND THEN RETURN 0;
            WHEN OTHERS THEN RAISE;
    END; 
    IF sec_id IS NULL THEN
        RETURN 0;
    END IF;
    OPEN aces FOR
        SELECT group_id, user_id, context_id, p_remove AS isauth,
        	p_x_super_deny, p_remove_direct AS isauth_direct
        FROM cm_ace
        WHERE security_id=sec_id
            AND (user_id=p_u OR (
                p_glist IS NOT NULL
                AND group_id IS NOT NULL
                AND INSTR(','||p_glist||',',','||group_id||',')>0 
            ))
            AND (context_id IS NULL OR (
                p_ctxlist IS NOT NULL
                AND INSTR(','||p_ctxlist||',',','||context_id||',')>0
            ));
    IF p_nodeid=sec_id THEN
    	node_id_eq_sec_id:=TRUE;
    ELSE
    	node_id_eq_sec_id:=FALSE;
    END IF; 
    rc:=isauth(node_id_eq_sec_id,aces,p_u,p_glist,p_ctxlist);
    CLOSE aces;
    IF rc=0 THEN
        RETURN 0;
    END IF;
    FOR x IN (
        SELECT DISTINCT n.security_id sec_id, n.id id
        FROM cm_node_parents np JOIN cm_node n ON n.id=np.node_id
        WHERE np.parent_id=p_nodeid
        ) LOOP
        IF x.sec_id IS NULL THEN
            RETURN 0;
        END IF;
        IF x.sec_id<>sec_id OR x.id<>p_nodeid THEN
            OPEN aces FOR
                SELECT group_id, user_id, context_id, p_remove AS isauth,
                	p_x_super_deny, p_remove_direct AS isauth_direct
                FROM cm_ace
                WHERE security_id=x.sec_id
                    AND (user_id=p_u OR (
                        p_glist IS NOT NULL
                        AND group_id IS NOT NULL
                        AND INSTR(','||p_glist||',',','||group_id||',')>0 
                    ))
                    AND (context_id IS NULL OR (
                        p_ctxlist IS NOT NULL
                        AND INSTR(','||p_ctxlist||',',','||context_id||',')>0
                    ));
            IF x.id=x.sec_id THEN
    			node_id_eq_sec_id:=TRUE;
    		ELSE
    			node_id_eq_sec_id:=FALSE;
    		END IF;
            rc:=isauth(node_id_eq_sec_id,aces,p_u,p_glist,p_ctxlist);
            CLOSE aces;
            IF rc=0 THEN
                RETURN 0;
            END IF;
        END IF;
    END LOOP;
    RETURN 1;
END premovetree;

FUNCTION pread(
	p_node_id IN NUMBER,
    p_secid IN NUMBER,
    p_u IN VARCHAR2,
    p_glist IN VARCHAR2,
    p_ctxlist IN VARCHAR2,
    p_allow_browse IN INTEGER
) RETURN INTEGER IS
    i INTEGER;
    victim INTEGER;
BEGIN
    iteration:=iteration+1;
    IF p_u IS NULL THEN
        RETURN 0;
    ELSIF p_secid IS NULL THEN
        RETURN 1;
    ELSIF NOT(p_secid IS NOT NULL AND p_node_id IS NOT NULL) THEN
    	RETURN pread_(p_node_id,p_secid,p_u,p_glist,p_ctxlist,p_allow_browse);    
    ELSIF cached_userid<>p_u OR cached_glist<>p_glist OR cached_ctxlist<>p_ctxlist 
        OR cached_allow_browse<>p_allow_browse THEN
        securityCache.DELETE;
        cached_userid:=p_u;
        cached_glist:=p_glist;
        cached_ctxlist:=p_ctxlist;
        cached_allow_browse:=p_allow_browse;
        iteration:=1;
        RETURN pread_(p_node_id,p_secid,p_u,p_glist,p_ctxlist,p_allow_browse);
    ELSIF p_secid<>p_node_id AND securityCache.EXISTS(-p_secid) THEN
        securityCache(-p_secid).used:=iteration;
        RETURN securityCache(-p_secid).pread;
    ELSIF p_secid=p_node_id AND securityCache.EXISTS(p_secid) THEN
        securityCache(p_secid).used:=iteration;
        RETURN securityCache(p_secid).pread;
    END IF;
    IF securityCache.COUNT=max_entries THEN
        victim:=securityCache.FIRST;
        i:=securityCache.NEXT(victim);
        WHILE i IS NOT NULL LOOP
            IF securityCache(i).used<securityCache(victim).used THEN
                victim:=i;
            END IF;
            i:=securityCache.NEXT(i);
        END LOOP;
        securityCache.DELETE(victim);
    END IF;
    RETURN pread_(p_node_id,p_secid,p_u,p_glist,p_ctxlist,p_allow_browse);
END pread;

END %%SECURITY-PKG%%;
/