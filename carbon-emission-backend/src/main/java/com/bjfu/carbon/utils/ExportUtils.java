package com.bjfu.carbon.utils;

public class ExportUtils {
	public abstract static class ExportStrategy {
		public abstract String export();
	}

	public enum ExportStrategyEnum {
		WORD,
		PDF;
	}

	public static String exportByStrategy(ExportStrategyEnum exportStrategy) {
		switch (exportStrategy) {
			case WORD:
				return new WordExport().export();

			case PDF:
				return new PdfExport().export();

			default:
				throw new IllegalArgumentException("Unsupported export strategy type: " + exportStrategy);
		}
	}

	public static class WordExport extends ExportStrategy {
		@Override
		public String export() {
			return "Word Export";
		}
	}

	public static class PdfExport extends ExportStrategy {
		@Override
		public String export() {
			return "Pdf Export";
		}
	}
}
