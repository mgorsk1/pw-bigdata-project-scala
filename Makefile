PROJECT_NAME=pw-bd-project

SSH_KEY='/home/mgorski/Dokumenty/keys/ovh/id_rsa'

MASTER_HOST='10.112.112.11'
MASTER_USER='mgorski'
MASTER_DIR='/home/mgorski/pw-bd-project-scala/'

sync-repo:
	rsync -avh --chmod=0755 --progress --cvs-exclude --include '.env' --exclude '__pycache__' --exclude 'tmp' --exclude '.docker' --exclude 'venv' --exclude 'tests' --exclude '.vscode' --exclude='*.pyc' --exclude 'log' --exclude 'jar' --exclude '.idea' -e "ssh -i $(SSH_KEY)" ./* ${MASTER_USER}@$(MASTER_HOST):$(MASTER_DIR)/${PROJECT_FOLDER}

