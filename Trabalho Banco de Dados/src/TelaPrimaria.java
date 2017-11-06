
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import br.edu.opet.util.Leitor;
import redis.clients.jedis.Jedis;

public class TelaPrimaria {
	private static DateTimeFormatter sFormatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static DateTimeFormatter sFormatadorHora = DateTimeFormatter.ofPattern("dd/MM/yyyy:HH:mm:ss");

	public static void main(String[] args) {
		// Conectando no Redis server - localhost
		Jedis jedis = new Jedis("localhost");

		loop1: while (true) {
			// Menu Iniciar

			System.out.println();
			System.out.println("-------- MENU 1 --------");
			System.out.println("1 - Criar Novo Usuário");
			System.out.println("2 - Fazer Login");
			System.out.println("3 - Sair do Programa");

			System.out.println();

			int tMenu = Leitor.readInt("O que você deseja fazer? ");

			casos1: switch (tMenu) {

			case 1:// Criando novo usuario

				LocalDate tDatatAtualCadastro = LocalDate.now();

				System.out.println("Novo Usuário");
				String tNome = Leitor.readString("Nome: ");
				if (tNome == "") {
					break casos1;
				}
				while (true) {

					String tApelido = Leitor.readString("Apelido: ");
					if (tApelido == "") {
						break casos1;
					}

					String tApelidoBanco1 = jedis.hget("Usuario:" + tApelido, "Apelido");

					if (!(tApelidoBanco1 == null)) {
						System.out.println("Este apelido de usuário já existe! Por favor, tente outro.");
						System.out.println();
					} 
						jedis.hset("Usuario:" + tApelido, "Nome", tNome);
						jedis.hset("Usuario:" + tApelido, "Apelido", tApelido);
						jedis.hset("Usuario:" + tApelido, "DataCadastro", tDatatAtualCadastro.format(sFormatador));
						break casos1;
					}

			case 2:

				while (true) {
					System.out.println();
					System.out.println("LOGIN");
					String tApelidoDigitado = Leitor.readString("Apelido:");
					if (tApelidoDigitado == "") {
						break casos1;
					}

					String tApelidoBanco = jedis.hget("Usuario:" + tApelidoDigitado, "Apelido");

					if (tApelidoBanco == null || !tApelidoBanco.equals(tApelidoDigitado)) {
						System.out.println(" Apelido inválido");
					} else {
						while (true) {

							System.out.println();
							System.out.println("------- MENU 2 -------");
							System.out.println("1 - Enviar Mensagem");
							System.out.println("2 - Caixa de Entradas");
							System.out.println("3 - Caixa de Saída");
							System.out.println("4 - Visualizar Dados");
							System.out.println("5 - Voltar para a Tela Inicial");
							System.out.println();

							int tOpcao = Leitor.readInt("O que você desejá fazer? ");

							casos2: switch (tOpcao) {

							case 1:

								LocalDateTime tDataAtualMensagem = LocalDateTime.now();

								System.out.println();
								String tPara = Leitor.readString("Para:");

								if (tPara == "") {
									break casos2;
								}

								String tMensagem = Leitor.readString("Mensagem:");

								if (tMensagem == "") {
									break casos2;
								}

								jedis.sadd(tApelidoDigitado + ":" + tDataAtualMensagem.format(sFormatadorHora) + ":Para",
										tPara);
								jedis.set(tApelidoDigitado + ":" + tDataAtualMensagem.format(sFormatadorHora) + ":De",
										tApelidoDigitado);
								jedis.set(tApelidoDigitado + ":" + tDataAtualMensagem.format(sFormatadorHora) + ":Mensagem",
										tMensagem);

								Long nSaidas = jedis.incr(tApelidoDigitado + "--saida");
								jedis.zadd(tApelidoDigitado + "--saida ", nSaidas,
										tApelidoDigitado + ":" + tDataAtualMensagem.format(sFormatadorHora));

								String[] textoSeparado = tPara.split(",");
								// System.out.println(Arrays.toString(textoSeparado));
								for (int i = 0; i < textoSeparado.length; i++) {
									// System.out.println(textoSeparado[i]);

									Long nEntradas = jedis.incr(textoSeparado[i] + "--entr ");

									jedis.zadd(textoSeparado[i] + "--entr  ", nEntradas,
											tApelidoDigitado + ":" + tDataAtualMensagem.format(sFormatadorHora));
								}
								break casos2;

							case 2:

								System.out.println("Caixa de Entrada");

								System.out.println(jedis.zrange(tApelidoDigitado + ": entregue ", 0, -1));

								Long tVisualisar = Leitor.readLong("Visualisar mensagem:");
								if (tVisualisar == 0) {
									break casos2;
								} else {

									Set<String> tMensagemVista = jedis.zrange(tApelidoDigitado + ": entregue ", tVisualisar - 1,
											tVisualisar - 1);

									String stringCortando = (tMensagemVista).toString();
									String tMensagemVista2 = stringCortando.substring(1, stringCortando.length() - 1);

									System.out.println(
											jedis.get(tMensagemVista2 + ":De") + ": " + jedis.get(tMensagemVista2 + ":Mensagem"));
									System.out.println();

									String tResposta = Leitor.readString("Resposta:");

									if (tResposta == "") {
										break casos2;
									} else {

										Long nEntradasRespostas = jedis.incr(tMensagemVista2 + ":respostas");
										jedis.zadd(tMensagemVista + ":respostas", nEntradasRespostas, tResposta);
									}
									/* ZADD ZE:20102017092745:RESPOSTAS 1 "NEMEU:20102017092855" */
								}
								break casos2;

							case 3:

								System.out.println("Caixa de Saída");

								System.out.println(jedis.zrange(tApelidoDigitado + ":saidas", 0, -1));

								Long tVisualisarSaida = Leitor.readLong("Visualisar mensagem:");
								if (tVisualisarSaida == 0 || tVisualisarSaida < 0) {
									break casos2;
								} else {
									Set<String> tMensagemVista = jedis.zrange(tApelidoDigitado+":saidas",
											tVisualisarSaida - 1, tVisualisarSaida - 1);

									String stringCortando = (tMensagemVista).toString();
									String tMensagemVista2 = stringCortando.substring(1, stringCortando.length() - 1);

									System.out.println(jedis.smembers(tMensagemVista2 +":Para"));
									System.out.println(jedis.get(tMensagemVista2+":Mensagem"));
									System.out.println();
								}
								break casos2;

							case 4:
								System.out.println();
								System.out.println("Visualizar os Dados");

								System.out.println("Apelido: "+jedis.hget("Usuario:"+tApelidoDigitado,"Apelido"));
								System.out.println("Nome: " + jedis.hget("Usuario:"+tApelidoDigitado,"Nome"));
								System.out.println("Data de Cadastro: "
										+ jedis.hget("Usuario:" + tApelidoDigitado, "DataCadastro"));
								System.out.println();
								break casos2;

							case 5:
								break casos1;
							}
						}
					}
				}

			case 3:
				break loop1;
			}

		}
		jedis.close();
	}
}
