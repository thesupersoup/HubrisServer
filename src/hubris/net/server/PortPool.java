package hubris.net.server;

/**
 * Holds ports in an int array and provides them sequentially when requested
 */
public class PortPool {
	public static final int ERROR = -1;

	private int[] portArr = null;
	private int index;

	public PortPool(int[] nArr){
		portArr = nArr;
		index = 0;
	}

	/**
	 * Get the next port in the array, will loop when the end of the array is reached
	 * @return
	 */
	public int getPort() {
		int port = ERROR;

		if(portArr != null && portArr.length > 0) {
			port = portArr[index];
			index++;
			if(index == portArr.length)
				index = 0;
		}

		return port;
	}
}
