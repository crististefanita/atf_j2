CREATE USER IF NOT EXISTS 'atf_reader'@'%' IDENTIFIED BY 'atf-lab-reader';
GRANT SELECT ON eventuate.* TO 'atf_reader'@'%';
