#!/usr/bin/env python3
"""Lecteur minimal de fichiers .class (JVMS 4), utilisé par les scripts de correctif et de
vérification du dépôt. Suffisant pour relire un constant-pool et contrôler la structure d'un
fichier sans JVM ni bibliothèque externe.

Les entrées du constant-pool sont renvoyées sous forme (tag, debut, fin, payload) afin qu'une
entrée puisse être réécrite puis le fichier reconstruit octet par octet.
"""

import struct

UTF8 = 1
CLASS = 7
STRING = 8
INTEGER = 3
FLOAT = 4
LONG = 5
DOUBLE = 6
FIELDREF = 9
METHODREF = 10
IMETHODREF = 11
NAMEANDTYPE = 12
METHODHANDLE = 15
METHODTYPE = 16
DYNAMIC = 17
INVOKEDYNAMIC = 18
MODULE = 19
PACKAGE = 20

TWO_BYTE_TAGS = (CLASS, STRING, METHODTYPE, MODULE, PACKAGE)
FOUR_BYTE_TAGS = (INTEGER, FLOAT, FIELDREF, METHODREF, IMETHODREF, NAMEANDTYPE, DYNAMIC, INVOKEDYNAMIC)
WIDE_TAGS = (LONG, DOUBLE)


class ClassFormatError(Exception):
    pass


def parse_constant_pool(data: bytes, offset: int, count: int):
    """Retourne (entries, offset_apres_le_constant_pool).

    entries = [(index_pool, tag, debut, fin, payload_utf8)]. L'index est conservé car les
    constantes Long et Double occupent deux slots du constant-pool.
    """
    entries = []
    number = 1
    limit = offset
    while number < count:
        start = limit
        tag = data[limit]
        limit += 1
        payload = None
        if tag == UTF8:
            (length,) = struct.unpack_from(">H", data, limit)
            limit += 2
            payload = data[limit : limit + length]
            limit += length
        elif tag in TWO_BYTE_TAGS:
            limit += 2
        elif tag == METHODHANDLE:
            limit += 3
        elif tag in FOUR_BYTE_TAGS:
            limit += 4
        elif tag in WIDE_TAGS:
            limit += 8
            number += 1
        else:
            raise ClassFormatError(f"tag de constant-pool inconnu: {tag} (offset {start})")
        if limit > len(data):
            raise ClassFormatError(f"constante {number} ({tag}) hors du fichier")
        entries.append((number, tag, start, limit, payload))
        number += 1
    return entries, limit


def utf8_values(data: bytes):
    """Toutes les chaînes CONSTANT_Utf8 du fichier."""
    entries, _ = parse_header(data)
    return [e[4].decode("utf-8", "replace") for e in entries if e[1] == UTF8]


def replace_utf8(data: bytes, replacements, expect_changes=None, exact=False):
    """Remplace des sous-chaînes dans les constantes CONSTANT_Utf8 puis reconstruit le fichier.

    Avec ``exact=True``, une règle ne s'applique que sur une entrée dont la valeur est
    *entièrement* égale à la clé : indispensable pour renommer un nom nu (``HoloEasy``, ``Vault``)
    sans toucher aux phrases de log qui le contiennent (« HoloEasy not found. Disabling plugin. »).

    Les remplacements sont appliqués sur les octets (et non sur des chaînes décodées) : un
    constant-pool utilise du UTF-8 modifié (MUTF-8, cf. JVMS 4.4.7) que ``bytes.decode()`` ne
    tolère pas forcément. Aucun octet de bytecode n'est touché, seuls les entrées Utf8 et le
    constant-pool sont réémis, puis le fichier est revalidé intégralement.

    :param replacements: mapping ``octets_anciens -> octets_nouveaux``
    :param expect_changes: si fourni, nombre d'entrées modifiées attendu
    :param exact: si vrai, la clé doit égaler toute la constante (pas seulement la contenir)
    :return: (nouvelles_octets, nombre_d_entrees_modifiees)
    """
    entries, (minor, major, cp_count, cp_end) = parse_header(data)
    rebuilt = bytearray(data[:10])
    changed = 0
    for _index, tag, start, end, payload in entries:
        if tag == UTF8 and payload:
            new_payload = payload
            for old, new in replacements.items():
                if exact:
                    if new_payload == old:
                        new_payload = new
                elif old in new_payload:
                    new_payload = new_payload.replace(old, new)
            if new_payload != payload:
                changed += 1
                rebuilt += struct.pack(">BH", UTF8, len(new_payload)) + new_payload
                continue
        rebuilt += data[start:end]
    rebuilt += data[cp_end:]
    patched = bytes(rebuilt)
    new_major, new_cp_count = walk(patched)
    if (new_major, new_cp_count) != (major, cp_count):
        raise ClassFormatError("en-tête ou constante pool modifiés par la réécriture")
    if expect_changes is not None and changed != expect_changes:
        raise ClassFormatError(f"{changed} entrée(s) modifiée(s), {expect_changes} attendues")
    return patched, changed


def class_names(data: bytes):
    """Retourne (this_class, super_class) sous forme de noms internes, et l'index de constant-pool."""
    entries, (minor, major, cp_count, offset) = parse_header(data)
    by_index = {e[0]: e for e in entries}
    (access_flags, this_class, super_class, interfaces_count) = struct.unpack_from(">HHHH", data, offset)

    def utf8(index):
        entry = by_index.get(index)
        if entry is None or entry[1] != UTF8:
            return "?"
        return entry[4].decode("utf-8", "replace")

    def class_name(index):
        entry = by_index.get(index)
        if entry is None or entry[1] != CLASS:
            return "?"
        return utf8(struct.unpack_from(">H", data, entry[2] + 1)[0])

    return class_name(this_class), class_name(super_class), entries, by_index, utf8


def parse_header(data: bytes):
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ClassFormatError("signature CAFEBABE absente")
    (minor, major, cp_count) = struct.unpack_from(">HHH", data, 4)
    entries, after_cp = parse_constant_pool(data, 10, cp_count)
    return entries, (minor, major, cp_count, after_cp)


def walk(data: bytes):
    """Parcourt la totalité de la structure du fichier et vérifie que les longueurs déclarées
    collent exactement à la taille du fichier. Retourne (major, cp_count)."""
    entries, (minor, major, cp_count, offset) = parse_header(data)
    (access_flags, this_class, super_class, interfaces_count) = struct.unpack_from(">HHHH", data, offset)
    offset += 8 + 2 * interfaces_count
    if this_class >= cp_count or super_class >= cp_count:
        raise ClassFormatError("index de constant-pool hors bornes pour this/super class")
    for section in ("fields", "methods"):
        (count,) = struct.unpack_from(">H", data, offset)
        offset += 2
        for _ in range(count):
            (_flags, _name, _desc, attrs) = struct.unpack_from(">HHHH", data, offset)
            offset += 8
            offset = _skip_attributes(data, offset, attrs)
    (attrs,) = struct.unpack_from(">H", data, offset)
    offset += 2
    offset = _skip_attributes(data, offset, attrs)
    if offset != len(data):
        raise ClassFormatError(f"{len(data) - offset} octet(s) non expliqué(s) en fin de fichier")
    return major, cp_count


def _skip_attributes(data: bytes, offset: int, count: int) -> int:
    for _ in range(count):
        (name_index, length) = struct.unpack_from(">HI", data, offset)
        offset += 6 + length
        if offset > len(data):
            raise ClassFormatError(f"attribut {name_index} déclaré sur {length} octets hors du fichier")
    return offset
