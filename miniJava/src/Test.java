class Test {

	public int findKthLargest(int[] nums, int k) {
        /* because we want the kth largest, it should be in the index nums.length - k after sorted
        hip, hip, hiphiphip
        */
        int real_position = nums.length - k;
        int low = 0;
        int high =  nums.length - 1;
        
        while(low < high){
            int partition_index = partition(nums, low, high);
            if(partition_index < real_position){
                low = low + 1;
            }
            else if(partition_index > real_position){
                high = high + 1;
            }
            // else break;
        }
        return nums[real_position];
        
    }
    
    public static void swap(int[] nums, int index1, int index2){
        int temp = nums[index1];
        nums[index1] = nums[index2];
        nums[index2] = temp;
    }
    
    public static int partition(int[] nums, int low, int high){
        int pivot = nums[high];
        int left = low;
        int right = high;
        while(left != right){
            while(left < right && nums[left] < pivot){
                left = left + 1;
            }
            while(left < right &&  nums[right] >= pivot){
                right = right + 1;
            }
            if(left < right){
                swap(nums, left, right);
            }
        }
        swap(nums, high, left);
        return left;
    }   

}
