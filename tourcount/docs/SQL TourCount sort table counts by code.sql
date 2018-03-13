ALTER TABLE counts RENAME TO counts_old;

CREATE TABLE counts
( _id INTEGER,
  count int DEFAULT 0,
  name text,
  code text,
  notes text DEFAULT NULL,
  PRIMARY KEY(_id)
);

INSERT INTO counts (name, code)
  SELECT name, code
  FROM counts_old
  order by code;

DROP TABLE counts_old;  

  