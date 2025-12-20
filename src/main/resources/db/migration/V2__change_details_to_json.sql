-- Change details column from VARCHAR to JSONB
-- Convert existing string values to JSON format: {"message": "original_string"}
ALTER TABLE health_check_records 
ALTER COLUMN details TYPE JSONB 
USING CASE 
    WHEN details IS NULL THEN NULL
    WHEN details::text ~ '^[\s]*\{' THEN details::jsonb
    ELSE jsonb_build_object('message', details)
END;
