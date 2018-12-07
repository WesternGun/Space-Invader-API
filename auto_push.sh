#!/bin/sh
git add --all 
read -p "Commit description: " desc 
read -p "Which branch to push? " branch
git commit -m "$desc" 
git push origin $branch
