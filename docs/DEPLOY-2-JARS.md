# Déployer les deux jars

`deploy.yml` ne copie qu'un seul fichier (`find target -maxdepth 1 -name "*.jar" | head -n 1`). Depuis
l'ajout de `ValoriaEconomy`, il faut déposer **les deux** jars dans `plugins/`.

Remplacer l'étape `Envoyer le plugin sur MCServerHost via SFTP` par :

```yaml
      - name: Envoyer les plugins sur MCServerHost via SFTP
        env:
          SFTP_HOST: ${{ secrets.SFTP_HOST }}
          SFTP_PORT: ${{ secrets.SFTP_PORT }}
          SFTP_USERNAME: ${{ secrets.SFTP_USERNAME }}
          SSHPASS: ${{ secrets.SFTP_PASSWORD }}
          JAR_PATH: ${{ steps.select-jar.outputs.jar_path }}
        run: |
          sudo apt-get update -y && sudo apt-get install -y sshpass
          ECONOMY_JAR=$(find target -maxdepth 1 -type f -name "ValoriaEconomy-*.jar" | head -n 1)
          sshpass -e sftp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P "$SFTP_PORT" "$SFTP_USERNAME@$SFTP_HOST" <<EOF
          put "$JAR_PATH" plugins/
          EOF
          if [ -n "$ECONOMY_JAR" ]; then
            sshpass -e sftp -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -P "$SFTP_PORT" "$SFTP_USERNAME@$SFTP_HOST" <<EOF
          put "$ECONOMY_JAR" plugins/
          EOF
          fi
```

Et l'étape `Identifier le JAR final du plugin` doit exclure le jar d'économie, sinon `find … | head -1`
peut choisir le mauvais :

```yaml
          FINAL_JAR=$(find target -maxdepth 1 -type f -name "*.jar" \\
            ! -name "original-*" ! -name "*-sources.jar" ! -name "*-javadoc.jar" \\
            ! -name "ValoriaEconomy-*" | head -n 1)
```

Note : `.github/workflows/` est protégé côté application GitHub de cette session (permission
`workflows` refusée), donc ces deux modifications sont à coller par un humain — soit via l'éditeur web
du dépôt, soit en local avec `git add .github/workflows/deploy.yml && git push`.
