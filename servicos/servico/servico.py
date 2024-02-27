from flask import Flask, jsonify
import json

# algumas constantes
ALIVE = True
VERSAO = "1.0"

# leitura de banco de dados estatico (deve ser substituido por um banco de dados real)
disciplinas = []
with open("dados.json", "r") as arquivo:
    dados = json.load(arquivo)

    VERSAO = dados["versao"]
    disciplinas.append(dados["disciplinas"])


# definicao de rotas do servico
servico = Flask("EducaRA")

@servico.route("/alive", methods=["GET"])
def alive():
    return jsonify(alive = ALIVE, versao = VERSAO)

@servico.route("/disciplinas", methods=["GET"])
def get_disciplinas():
    return jsonify(disciplinas[0])

@servico.route("/aulas/<int:idDisciplina>", methods=["GET"])
def get_aulas(idDisciplina):
    aulas = []

    for disciplina in disciplinas[0]:
        if disciplina["id"] == idDisciplina:
            aulas.append(disciplina["aulas"])

            break

    return jsonify(aulas[0])

@servico.route("/conteudos/<int:idAula>", methods=["GET"])
def get_conteudos(idAula):
    conteudos = []

    for disciplina in disciplinas[0]:
        aulas = disciplina["aulas"]

        for aula in aulas:
            if aula["id"] == idAula:
                conteudos.append(aula["conteudos"])

                break

    return jsonify(conteudos[0])

@servico.route("/conteudo/<int:idConteudo>", methods=["GET"])
def get_conteudo(idConteudo):
    conteudo = {}

    for disciplina in disciplinas[0]:
        aulas = disciplina["aulas"]

        for aula in aulas:
            conteudos = aula["conteudos"]

            for conteudo in conteudos:
                if conteudo["id"] == idConteudo:
                    conteudo = aula["conteudos"]

                    break

    return jsonify(conteudo)


if __name__ == "__main__":
    servico.run(host="0.0.0.0", port=3000, debug=True)
