#include <mpi.h>
#include <stdio.h>
#include <stdlib.h> 
#include <string.h>
#include <math.h>
#define GRAY 1
#define COLOR 3

typedef struct{
	int type;
	int width;
	int height;
	int nr_bytes;
	unsigned char* bytes;
}Image;


float smooth[3][3] = {{1.0/9, 1.0/9, 1.0/9}, {1.0/9, 1.0/9, 1.0/9}, {1.0/9, 1.0/9, 1.0/9}};
float blur[3][3] = {{1.0/16, 1.0/8, 1.0/16}, {1.0/8, 1.0/4, 1.0/8}, {1.0/16, 1.0/8, 1.0/16}};
float sharpen[3][3] = {{0, -2.0/3, 0}, {-2.0/3, 11.0/3, -2.0/3}, {0, -2.0/3, 0}};
float mean[3][3] = {{-1.0, -1.0, -1.0}, {-1.0, 9, -1.0}, {-1.0, -1.0, -1.0}};
float emboss[3][3] = {{0, -1.0, 0}, {0, 0, 0}, {0, 1.0, 0}};


// Function that selects one of the matrices above (using the mat pointer)
void get_mat(char* filter_type, float (**mat)[3]){

    if(strcmp(filter_type, "smooth") == 0){
        *mat = smooth;
        return;
    }
    if(strcmp(filter_type, "blur") == 0){
        *mat = blur;
        return;
    }
    if(strcmp(filter_type, "sharpen") == 0){
        *mat = sharpen;
        return;
    }
    if(strcmp(filter_type, "mean") == 0){
        *mat =  mean;
        return;
    }
    if(strcmp(filter_type, "emboss") == 0){
        *mat =  emboss;
        return;
    }
}

// used to read lines from a file ( as text, not binary)
char* next_str(FILE* file){

	char* str = NULL;
	size_t len = 0;
	ssize_t bytes_read;

	do{
		bytes_read = getline(&str, &len, file);
	}while(bytes_read == 0 || bytes_read > 0 && str[0] == '#');

	return str;
}

// load an image
Image* read_img(char* filename){

	FILE* file;
	Image* image =(Image*)malloc(sizeof(Image));
	if((file = fopen(filename, "r")) == NULL){
		return NULL;
	}

	char *type = next_str(file);
	if(strcmp(type, "P6\n") == 0 || strcmp(type, "P6") == 0) {
		image->type = COLOR;
	} else{
		image->type = GRAY;
	}

	char* dimensions = next_str(file);
	image->width = atoi(strtok(dimensions, " \n"));
	image->height = atoi(strtok(NULL, " \n"));
	
	int max_val = atoi(next_str(file));

    image->nr_bytes = image->width * image->height * image->type;
	image->bytes = (unsigned char*)malloc( image->nr_bytes * sizeof(unsigned char));
	fread(image->bytes, image->nr_bytes * sizeof(unsigned char), 1, file);
	
	fclose(file);
	return image;
}

// write an image from an Image object
void write_img(char* filename, Image* image){
	
	FILE* out = fopen(filename, "wb");
	
	char str[20];
	if(image->type == COLOR){
		strcpy(str, "P6");
	} else{
		strcpy(str, "P5");
	}

	fprintf(out, "%s\n%d %d\n255\n", str, image->width, image->height);
	
	fwrite(image->bytes, image->nr_bytes * sizeof(unsigned char), 1, out);
	fclose(out);
}


int main(int argc, char * argv[]){
	int rank;
	int nr_procese;

	MPI_Init(&argc, &argv);
	MPI_Status status;
	MPI_Request request;

	MPI_Comm_rank(MPI_COMM_WORLD, &rank);
	MPI_Comm_size(MPI_COMM_WORLD, &nr_procese);

    int root = 0;
    int header[4];
    Image* img;

    // only the root process read the image
    if(rank == root){
        img = read_img(argv[1]);
        header[0] = img->type;
        header[1] = img->width;
        header[2] = img->height;
        header[3] = img->nr_bytes;
    }
    // the header is broadcasted to all processes
    MPI_Bcast (header, 4, MPI_INT, root, MPI_COMM_WORLD);

    int type = header[0];
    int width = header[1];
    int height = header[2];
    int nr_bytes = header[3];

    // buffer for the image
    unsigned char* image;
    if(rank != root)
     image = (unsigned char*)malloc(nr_bytes * sizeof(unsigned char));
    else{
        image = img->bytes;
    }

    // iterate through the filters
    int nr_filters = argc-3;
    for(int i = 0; i < nr_filters; i++){
        MPI_Barrier (MPI_COMM_WORLD);

        char* filter_type = argv[i + 3];

        //select corresponding matrix for the filter type
        float (*mat)[3] = NULL;
        get_mat(filter_type, &mat);

        if(mat == NULL){
            continue;
        }

        // The root broadcasts the hole image to every process
        MPI_Bcast(image, nr_bytes, MPI_UNSIGNED_CHAR, root, MPI_COMM_WORLD);
        int nr_pixels = width * height;

        // The pixels of the resulted image are divided equally
        int start = rank * ceil((float)nr_pixels/nr_procese);
        int end = fmin(nr_pixels, (rank+1) * ceil((float)nr_pixels/nr_procese));
        int size = type*(end - start);
        unsigned char chunk[size];
        for(int idx = start; idx < end; idx++){
            // calculate line, column of the pixel
            // calculate indexes of the matrix that surrounds the pixel
            int line = idx / width;
            int column = idx % width;
            int line_bound_down = line > 0 ? line-1 : 0;
            int line_bound_up = line < height-1 ? line+1 : height-1;
            int column_bound_down = column > 0 ? column-1 : 0;
            int column_bound_up = column < width-1 ? column+1 : width-1;

            // repeat for every channel of the image
            // type is 1 for grayscale images and 3 for RGB images
            for (int k = 0; k < type; k++){
                // new value for pixel is calculated
                float sum = 0;
                for(int i = line_bound_down; i <= line_bound_up; i++){
                    for(int j = column_bound_down; j <= column_bound_up; j++){
                        sum += mat[i - line + 1][j - column  + 1] * image[(width * i + j)*type + k];
                    }
                }

                 chunk[type*(idx - start) + k] = sum;

                 if(sum < 0 ){
                    chunk[type*(idx - start) + k] = 0;
                 }

                 if(sum > 255 ){
                    chunk[type*(idx - start) + k] = 255;
                 }


             }

        }

        // every process sends it's part of the image to the root process
        MPI_Gather ( chunk, size, MPI_UNSIGNED_CHAR, (void*)(image + start), size, MPI_UNSIGNED_CHAR, 0, MPI_COMM_WORLD );

    }


    if(rank == root){
        img->bytes = image;
    }else{
        free(image);
    }

    if(rank == root){
	    write_img(argv[2], img);
        free(img->bytes);
        free(img);
	}

	MPI_Finalize();
	return 0;
}
