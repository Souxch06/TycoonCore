#!/usr/bin/env python3
"""Publication du detail d'un echec de controle dans le resume du job GitHub Actions.

Pourquoi ce module : dans ce depot, la preuve d'un build ne peut pas passer par le journal brut d'un
job (il est servi par un stockage externe, generalement injoignable depuis un agent, et il est replie
dans l'interface). `$GITHUB_STEP_SUMMARY`, lui, est affiche en haut du job **et** sert les annotations
via l'API `/check-runs/{id}/annotations`. Un controle qui echoue sans y ecrire son detail est donc
inexploitable a distance : il rend le job rouge, et personne ne sait pourquoi.

Usage : `ci_publish.fail("titre", lignes)` sur le chemin d'erreur, `ci_publish.ok(texte)` pour la
trace de succes. Dehors de GitHub Actions (variable absente), les deux ne font rien : les scripts
restent utilisables en local.
"""

import os
import sys

MAX_LINES = 40


def _write(body: str) -> None:
    print(body)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if not summary:
        return
    try:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(body)
    except OSError as error:  # le resume n'est jamais une raison de faire echouer un controle
        print(f"(resume non ecrit : {error})", file=sys.stderr)


def fail(title: str, lines) -> None:
    """Publie une liste de lignes d'echec (strings), coupee a MAX_LINES pour rester lisible."""
    lines = [str(line).strip() for line in lines if str(line).strip()]
    body = [f"### {title} — {len(lines)} element(s)\n"]
    body += [f"- {line}" for line in lines[:MAX_LINES]]
    if len(lines) > MAX_LINES:
        body.append(f"- … {len(lines) - MAX_LINES} autres")
    _write("\n".join(body) + "\n")


def ok(text: str) -> None:
    _write(f"### {text}\n")
