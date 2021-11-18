CREATE FUNCTION %%USER-SCHEMA%%.PREAD(
	@p_node_id NUMERIC(19),
	@p_secid NUMERIC(19),
    @p_u NVARCHAR(64),
    @p_glist NVARCHAR(4000),
    @p_ctxlist NVARCHAR(4000),
    @p_allow_browse INTEGER
) RETURNS BIT AS 
BEGIN
	DECLARE @aces CURSOR;
    DECLARE @contextUserAllow BIT;
    DECLARE @contextUserDeny BIT;
    DECLARE @contextGroupAllow BIT;
    DECLARE @contextGroupDeny BIT;
    DECLARE @userAllow BIT;
    DECLARE @userDeny BIT;
    DECLARE @groupAllow BIT;
    DECLARE @groupDeny BIT;
	DECLARE @value BIT;
	DECLARE @ace_group_id NVARCHAR(64);
	DECLARE @ace_user_id NVARCHAR(64);
	DECLARE @ace_context_id NVARCHAR(64);
	DECLARE @ace_isauth BIT;
	DECLARE @ace_p_x_super_deny BIT;
	DECLARE @ace_isauth_direct BIT;
	SET @contextUserAllow=0;
    SET @contextUserDeny=0;
    SET @contextGroupAllow=0;
    SET @contextGroupDeny=0;
    SET @userAllow=0;
    SET @userDeny=0;
    SET @groupAllow=0;
    SET @groupDeny=0;
	IF @p_allow_browse=1
		SET @aces=CURSOR LOCAL FAST_FORWARD FOR
		SELECT GROUP_ID,USER_ID,CONTEXT_ID,
                CASE
                    WHEN P_READ=1 OR P_BROWSE=1 THEN 1
                    WHEN P_READ=0 OR P_BROWSE=0 THEN 0
                    ELSE NULL 
                END AS ISAUTH , P_X_SUPER_DENY,
                CASE
                	WHEN P_READ=1 THEN P_READ_DIRECT
                    WHEN P_BROWSE=1 THEN P_BROWSE_DIRECT
                    WHEN P_READ=0 THEN P_READ_DIRECT
                    WHEN P_BROWSE=0 THEN P_BROWSE_DIRECT
                    ELSE NULL
                END AS ISAUTH_DIRECT
            FROM %%USER-SCHEMA%%.CM_ACE 
            WHERE SECURITY_ID=@p_secid
                AND (USER_ID=@p_u OR (
                    @p_glist IS NOT NULL
                    AND GROUP_ID IS NOT NULL
                    AND CHARINDEX(','+GROUP_ID+',',','+@p_glist+',')>0 
                ))
                AND (CONTEXT_ID IS NULL OR (
                    @p_ctxlist IS NOT NULL 
                    AND CHARINDEX(','+CONTEXT_ID+',',','+@p_ctxlist+',')>0
                ));
	ELSE
		SET @aces=CURSOR LOCAL FAST_FORWARD FOR
		SELECT GROUP_ID,USER_ID,CONTEXT_ID,P_READ AS ISAUTH, P_X_SUPER_DENY, 
				P_READ_DIRECT AS ISAUTH_DIRECT
            FROM %%USER-SCHEMA%%.CM_ACE 
            WHERE SECURITY_ID=@p_secid
                AND (USER_ID=@p_u OR (
                    @p_glist IS NOT NULL 
                    AND GROUP_ID IS NOT NULL 
                    AND CHARINDEX(','+GROUP_ID+',',','+@p_glist+',')>0 
                ))
                AND (CONTEXT_ID IS NULL OR (
                    @p_ctxlist IS NOT NULL 
                    AND CHARINDEX(','+CONTEXT_ID+',',','+@p_ctxlist+',')>0
                ));
    OPEN @aces;
	FETCH NEXT FROM @aces INTO 
		@ace_group_id,
		@ace_user_id,
		@ace_context_id,
		@ace_isauth,
		@ace_p_x_super_deny,
		@ace_isauth_direct;
	WHILE @@FETCH_STATUS = 0
	BEGIN
		IF @ace_p_x_super_deny IS NOT NULL AND @ace_p_x_super_deny=1
			BEGIN
				CLOSE @aces
				DEALLOCATE @aces
				RETURN 0
			END
        IF @ace_isauth IS NULL
            GOTO CONTINUE_LABEL
        ELSE
            SET @value=@ace_isauth
        IF @ace_isauth_direct IS NOT NULL AND @ace_isauth_direct=1 AND @p_secid<>@p_node_id 
        	GOTO CONTINUE_LABEL
        IF @ace_group_id IS NOT NULL
			BEGIN
				IF @ace_context_id IS NOT NULL
					BEGIN
						IF @value=1
							SET @contextGroupAllow=1
						ELSE
							SET @contextGroupDeny =1
					END
				ELSE
					BEGIN
						IF @value=1
							SET @groupAllow=1
						ELSE
							SET @groupDeny=1
					END
				GOTO CONTINUE_LABEL
			END
        IF @ace_user_id IS NOT NULL
			BEGIN
				IF @ace_context_id IS NOT NULL
					BEGIN
						IF @value=1
							SET @contextUserAllow=1
						ELSE
							BEGIN	
								CLOSE @aces
								DEALLOCATE @aces
								RETURN 0
							END
					END
				ELSE
					BEGIN
						IF @value=1
							SET @userAllow=1
						ELSE
							SET @userDeny=1
				    END
			END
        CONTINUE_LABEL: FETCH NEXT FROM @aces INTO 
			@ace_group_id,
			@ace_user_id,
			@ace_context_id,
			@ace_isauth,
			@ace_p_x_super_deny,
			@ace_isauth_direct
	END
	CLOSE @aces
	DEALLOCATE @aces
    IF @contextUserDeny=1 RETURN 0
    IF @contextUserAllow=1 AND @contextGroupDeny=0 RETURN 1
    IF @contextGroupDeny=1 RETURN 0
    IF @contextGroupAllow=1 RETURN 1
    IF @userDeny=1 RETURN 0
    IF @userAllow=1 RETURN 1
    IF @groupDeny=1 RETURN 0
    IF @groupAllow=1 RETURN 1
    RETURN NULL
END